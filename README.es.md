[English](README.md) | [Português](README.pt-br.md)

# aws-cloud-formation

Proyecto con ejemplo de utilización del servicio AWS CloudFormation.

## 🚀 Despliegue Rápido

Para hacer el despliegue de la aplicación en AWS, utilice el script automatizado:

```bash
# Despliegue completo (DynamoDB + S3 + Lambda)
./aws/deploy.sh complete

# Despliegue solo del Lambda (requiere DynamoDB y S3 existentes)
./aws/deploy.sh

# Ver ayuda
./aws/deploy.sh help
```

El script automáticamente:
- ✅ Compila el proyecto con Maven
- ✅ Genera el uber JAR con todas las dependencias
- ✅ Sube el JAR al S3
- ✅ Valida el template CloudFormation
- ✅ Crea o actualiza el stack con los permisos necesarios

**Prerrequisitos:**
- AWS CLI configurado con credenciales válidas
- Maven instalado
- Bucket S3 `lambdaCode` creado (el script lo crea automáticamente si no existe)

**Región AWS:** Todos los recursos se crearán en la región **us-east-1** (N. Virginia) por defecto. Esta configuración está definida en el script de despliegue y en los templates CloudFormation.

Para más detalles sobre el proceso de despliegue, consulte el archivo [`DEPLOY.md`](DEPLOY.md).

## Estructura del Proyecto

### Directorio `aws/`

Contiene los scripts CloudFormation (YAML) y el script de despliegue automatizado para la creación y gestión de recursos AWS:

#### Templates CloudFormation

- **`aws/booksInfra-complete.yaml`**: Template completo que crea toda la infraestructura en un único stack:
  - Tabla DynamoDB `books` con clave compuesta (autor como partition key y género como sort key)
  - Bucket S3 `BookStorageBucket` para almacenamiento de archivos CSV
  - Función Lambda `booksEtl` con permisos para acceder a DynamoDB y S3
  - Role IAM con todos los permisos necesarios
  - Log Group de CloudWatch
  - Configuraciones de seguridad y cifrado

- **`aws/booksInfra-no-lambda.yaml`**: Template que crea solo la infraestructura básica (sin Lambda):
  - Tabla DynamoDB `books` con clave compuesta (autor como partition key y género como sort key)
  - Bucket S3 `BookStorageBucket` para almacenamiento de archivos CSV
  - Configuraciones de seguridad y cifrado para ambos recursos

- **`aws/lambda/booksEtl.yaml`**: Template para crear solo la función Lambda de ETL (Extract, Transform, Load) de libros. Requiere que DynamoDB y S3 ya existan.

#### Scripts y Recursos

- **`aws/deploy.sh`**: Script bash automatizado para hacer el despliegue completo de la aplicación. Vea la sección [Despliegue Rápido](#-despliegue-rápido) arriba.

- **`aws/literatura_brasileira.csv`**: Archivo CSV de ejemplo que contiene datos de libros de literatura brasileña para procesamiento por la función Lambda.

### Directorio `src/`

Contiene el código fuente Java de la función AWS Lambda creada por los scripts CloudFormation.

#### `src/main/java/com/books/`
Estructura del código Java organizada en paquetes:

- **`Main.java`**: Clase principal con método `main` para inicialización de la aplicación.
- **`application/`**: Capa de aplicación:
  - `BookApplication.java`: Clase principal que orquesta el flujo de procesamiento ETL de libros.
- **`core/`**: Clases de configuración e infraestructura:
  - `Configuration.java`: Carga y gestiona las configuraciones de la aplicación desde el archivo `application.properties`.
  - `DynamoDbClientFactory.java`: Factory para crear el cliente DynamoDB Enhanced Client.
- **`domain/`**: Capa de dominio:
  - **`book/`**: Modelos y mapeadores de libros:
    - `Book.java`: Modelo de dominio que representa un libro con atributos: título, autor, género y período.
    - `BookMapper.java`: Interfaz MapStruct para conversión entre `Book` (dominio) y `BookEntity` (persistencia).
  - **`csv/`**: Servicios de procesamiento CSV:
    - `CsvService.java`: Servicio responsable de hacer el parsing de archivos CSV a objetos `Book`.
  - **`exception/`**: Excepciones personalizadas:
    - `FileNotFoundException.java`: Excepción lanzada cuando un archivo no se encuentra en S3.
    - `ProcessingException.java`: Excepción genérica para errores durante el procesamiento.
- **`repository/`**: Capa de acceso a datos:
  - **`dynamo/`**: Repositorio y entidades DynamoDB:
    - `BookRepository.java`: Repositorio para operaciones CRUD con libros en DynamoDB (save, findByAutorAndGenero, delete).
    - `entity/BookEntity.java`: Entidad mapeada de la tabla DynamoDB con clave compuesta (autor como partition key y género como sort key).
  - **`s3/`**: Repositorio S3:
    - `S3Bucket.java`: Clase para lectura de archivos del bucket S3, soportando lectura como String o array de bytes.

#### `src/main/resources/`
Recursos de la aplicación:
- **`application.properties`**: Archivo de configuración que contiene:
  - `s3.url`: URL del bucket S3 donde se almacenan los archivos CSV.
  - `dynamodb.table.books`: Nombre de la tabla DynamoDB donde se persistirán los libros.

#### `src/test/`
Pruebas unitarias e de integración del código Java.

### Configuración del Proyecto

El proyecto utiliza Maven para gestión de dependencias y build. Configuraciones principales:

- **Java 21**: Runtime del Lambda
- **Maven Shade Plugin**: Configurado para generar uber JAR con todas las dependencias incluidas
- **AWS SDK 2.34.0**: Para integración con servicios AWS (Lambda, S3, DynamoDB)
- **Lombok**: Para reducción de boilerplate
- **MapStruct**: Para mapeo entre objetos de dominio y entidades
- **OpenCSV**: Para parsing de archivos CSV
- **Jakarta CDI**: Para inyección de dependencias

El archivo `pom.xml` contiene todas las dependencias y configuraciones necesarias. El plugin `maven-shade-plugin` está configurado para crear automáticamente un uber JAR durante el build.

## Funcionamiento de la Función Lambda

La función Lambda implementa un proceso ETL (Extract, Transform, Load) para procesar archivos CSV de libros almacenados en S3 y persistir los datos en DynamoDB.

### Flujo de Procesamiento

El flujo de procesamiento es orquestado por la clase `BookApplication` a través del método `processCsvFile(String fileName)`:

1. **Extract (Extracción)**:
   - La función recibe el nombre de un archivo CSV como parámetro.
   - `S3Bucket` lee el archivo del bucket S3 configurado, retornando el contenido como array de bytes.
   - La URL del S3 se resuelve desde la configuración (`s3.url`), extrayendo el bucket y el prefijo/path.
   - Si el archivo no se encuentra, se lanza una `FileNotFoundException`.

2. **Transform (Transformación)**:
   - `CsvService` hace el parsing del contenido CSV (array de bytes) a una lista de objetos `Book`.
   - Utiliza la biblioteca OpenCSV con anotaciones `@CsvBindByName` para mapear las columnas del CSV (titulo, autor, genero, periodo) a los atributos del modelo `Book`.
   - El parsing ignora líneas en blanco y espacios en blanco al inicio de las líneas.

3. **Load (Carga)**:
   - Cada objeto `Book` se convierte a `BookEntity` utilizando el `BookMapper` (MapStruct).
   - Los libros se guardan en DynamoDB a través del `BookRepository`, utilizando streams y lambdas para procesar cada elemento.
   - La tabla DynamoDB utiliza una clave compuesta:
     - **Partition Key**: `autor` (autor del libro)
     - **Sort Key**: `genero` (género del libro)
   - Cada libro se persiste individualmente, con logs detallados de éxito o error.

### Manejo de Errores

El código implementa manejo de errores en tres niveles:

- **`FileNotFoundException`**: Cuando el archivo no se encuentra en S3.
- **`ProcessingException`**: Para errores durante el parsing del CSV o persistencia en DynamoDB.
- **Errores inesperados**: Capturados y encapsulados en `ProcessingException` con mensajes descriptivos.

Todos los errores se registran utilizando SLF4J antes de ser relanzados.

### Configuración e Inyección de Dependencias

La aplicación utiliza Jakarta CDI (Contexts and Dependency Injection) para inyección de dependencias:

- Las clases marcadas con `@Singleton` se gestionan como singletons.
- Las dependencias se inyectan vía constructores utilizando `@RequiredArgsConstructor` de Lombok.
- `Configuration` carga las propiedades del archivo `application.properties` en la inicialización.
- `DynamoDbClientFactory` crea el cliente DynamoDB Enhanced Client necesario para operaciones en la base de datos.

### Arquitectura

La aplicación sigue una arquitectura en capas:

- **Capa de Aplicación**: `BookApplication` - orquesta el flujo de negocio.
- **Capa de Dominio**: Modelos (`Book`), servicios (`CsvService`) y excepciones específicas del dominio.
- **Capa de Repositorio**: Acceso a datos externos (S3 y DynamoDB).
- **Capa Core**: Configuraciones y factories para infraestructura AWS.

Esta separación facilita el mantenimiento, la testabilidad y la evolución del código.
