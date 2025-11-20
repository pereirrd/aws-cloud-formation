# aws-cloud-formation

Projeto com exemplo de utilização do serviço da AWS, Cloud Formation.

## Estrutura do Projeto

### Diretório `aws/`

Contém os scripts CloudFormation (YAML) para criação e gerenciamento dos recursos AWS. A estrutura está organizada por serviço:

#### `aws/booksInfra.yaml`
Template principal que orquestra a infraestrutura completa do cenário de livros, utilizando nested stacks para organizar os recursos.

#### `aws/dynamodb/`
- **`books.yaml`**: Script CloudFormation para criação da tabela DynamoDB de livros.

#### `aws/lambda/`
- **`booksEtl.yaml`**: Script CloudFormation para criação da função Lambda de ETL (Extract, Transform, Load) dos livros.

#### `aws/s3_bucket/`
- **`bookStorage.yaml`**: Script CloudFormation para criação do bucket S3 de armazenamento dos livros.

### Diretório `src/`

Contém o código fonte Java da função AWS Lambda criada pelos scripts CloudFormation.

#### `src/main/java/com/books/`
Estrutura do código Java organizada em pacotes:

- **`Main.java`**: Classe principal com método `main` para inicialização da aplicação.
- **`application/`**: Camada de aplicação:
  - `BookApplication.java`: Classe principal que orquestra o fluxo de processamento ETL dos livros.
- **`core/`**: Classes de configuração e infraestrutura:
  - `Configuration.java`: Carrega e gerencia as configurações da aplicação a partir do arquivo `application.properties`.
  - `DynamoDbClientFactory.java`: Factory para criação do cliente DynamoDB Enhanced Client.
- **`domain/`**: Camada de domínio:
  - **`book/`**: Modelos e mapeadores de livros:
    - `Book.java`: Modelo de domínio representando um livro com atributos: título, autor, gênero e período.
    - `BookMapper.java`: Interface MapStruct para conversão entre `Book` (domínio) e `BookEntity` (persistência).
  - **`csv/`**: Serviços de processamento CSV:
    - `CsvService.java`: Serviço responsável por fazer o parsing de arquivos CSV para objetos `Book`.
  - **`exception/`**: Exceções customizadas:
    - `FileNotFoundException.java`: Exceção lançada quando um arquivo não é encontrado no S3.
    - `ProcessingException.java`: Exceção genérica para erros durante o processamento.
- **`repository/`**: Camada de acesso a dados:
  - **`dynamo/`**: Repositório e entidades DynamoDB:
    - `BookRepository.java`: Repositório para operações CRUD com livros no DynamoDB (save, findByAutorAndGenero, delete).
    - `entity/BookEntity.java`: Entidade mapeada da tabela DynamoDB com chave composta (autor como partition key e gênero como sort key).
  - **`s3/`**: Repositório S3:
    - `S3Bucket.java`: Classe para leitura de arquivos do bucket S3, suportando leitura como String ou array de bytes.

#### `src/main/resources/`
Recursos da aplicação:
- **`application.properties`**: Arquivo de configuração contendo:
  - `s3.url`: URL do bucket S3 onde os arquivos CSV estão armazenados.
  - `dynamodb.table.books`: Nome da tabela DynamoDB onde os livros serão persistidos.

#### `src/test/`
Testes unitários e de integração do código Java.

## Funcionamento da Função Lambda

A função Lambda implementa um processo ETL (Extract, Transform, Load) para processar arquivos CSV de livros armazenados no S3 e persistir os dados no DynamoDB.

### Fluxo de Processamento

O fluxo de processamento é orquestrado pela classe `BookApplication` através do método `processCsvFile(String fileName)`:

1. **Extract (Extração)**:
   - A função recebe o nome de um arquivo CSV como parâmetro.
   - O `S3Bucket` lê o arquivo do bucket S3 configurado, retornando o conteúdo como array de bytes.
   - A URL do S3 é resolvida a partir da configuração (`s3.url`), extraindo o bucket e o prefixo/path.
   - Se o arquivo não for encontrado, uma `FileNotFoundException` é lançada.

2. **Transform (Transformação)**:
   - O `CsvService` faz o parsing do conteúdo CSV (array de bytes) para uma lista de objetos `Book`.
   - Utiliza a biblioteca OpenCSV com anotações `@CsvBindByName` para mapear as colunas do CSV (titulo, autor, genero, periodo) para os atributos do modelo `Book`.
   - O parsing ignora linhas em branco e espaços em branco no início das linhas.

3. **Load (Carga)**:
   - Cada objeto `Book` é convertido para `BookEntity` utilizando o `BookMapper` (MapStruct).
   - Os livros são salvos no DynamoDB através do `BookRepository`, utilizando streams e lambdas para processar cada item.
   - A tabela DynamoDB utiliza uma chave composta:
     - **Partition Key**: `autor` (autor do livro)
     - **Sort Key**: `genero` (gênero do livro)
   - Cada livro é persistido individualmente, com logs detalhados de sucesso ou erro.

### Tratamento de Erros

O código implementa tratamento de erros em três níveis:

- **`FileNotFoundException`**: Quando o arquivo não é encontrado no S3.
- **`ProcessingException`**: Para erros durante o parsing do CSV ou persistência no DynamoDB.
- **Erros inesperados**: Capturados e encapsulados em `ProcessingException` com mensagens descritivas.

Todos os erros são logados utilizando SLF4J antes de serem relançados.

### Configuração e Injeção de Dependências

A aplicação utiliza Jakarta CDI (Contexts and Dependency Injection) para injeção de dependências:

- Classes marcadas com `@Singleton` são gerenciadas como singletons.
- Dependências são injetadas via construtores utilizando `@RequiredArgsConstructor` do Lombok.
- O `Configuration` carrega as propriedades do arquivo `application.properties` na inicialização.
- O `DynamoDbClientFactory` cria o cliente DynamoDB Enhanced Client necessário para operações no banco.

### Arquitetura

A aplicação segue uma arquitetura em camadas:

- **Camada de Aplicação**: `BookApplication` - orquestra o fluxo de negócio.
- **Camada de Domínio**: Modelos (`Book`), serviços (`CsvService`) e exceções específicas do domínio.
- **Camada de Repositório**: Acesso a dados externos (S3 e DynamoDB).
- **Camada Core**: Configurações e factories para infraestrutura AWS.

Esta separação facilita a manutenção, testabilidade e evolução do código.
