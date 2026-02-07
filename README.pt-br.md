[English](README.md) | [Español](README.es.md)

# aws-cloud-formation

Projeto com exemplo de utilização do serviço da AWS Cloud Formation.

## 🚀 Deploy Rápido

Para fazer o deploy da aplicação na AWS utilize o script automatizado:

```bash
# Deploy completo (DynamoDB + S3 + Lambda)
./aws/deploy.sh complete

# Deploy apenas do Lambda (requer DynamoDB e S3 existentes)
./aws/deploy.sh

# Ver ajuda
./aws/deploy.sh help
```

O script automaticamente:
- ✅ Compila o projeto com Maven
- ✅ Gera o uber JAR com todas as dependências
- ✅ Faz upload do JAR para o S3
- ✅ Valida o template CloudFormation
- ✅ Cria ou atualiza o stack com as permissões necessárias

**Pré-requisitos:**
- AWS CLI configurado com credenciais válidas
- Maven instalado
- Bucket S3 `lambdaCode` criado (o script cria automaticamente se não existir)

**Região AWS:** Todos os recursos serão criados na região **us-east-1** (N. Virginia) por padrão. Esta configuração está definida no script de deploy e nos templates CloudFormation.

Para mais detalhes sobre o processo de deploy, consulte o arquivo [`DEPLOY.md`](DEPLOY.md).

## Estrutura do Projeto

### Diretório `aws/`

Contém os scripts CloudFormation (YAML) e o script de deploy automatizado para criação e gerenciamento dos recursos AWS:

#### Templates CloudFormation

- **`aws/booksInfra-complete.yaml`**: Template completo que cria toda a infraestrutura em um único stack:
  - Tabela DynamoDB `books` com chave composta (autor como partition key e gênero como sort key)
  - Bucket S3 `BookStorageBucket` para armazenamento dos arquivos CSV
  - Função Lambda `booksEtl` com permissões para acessar DynamoDB e S3
  - Role IAM com todas as permissões necessárias
  - Log Group do CloudWatch
  - Configurações de segurança e criptografia

- **`aws/booksInfra-no-lambda.yaml`**: Template que cria apenas a infraestrutura básica (sem Lambda):
  - Tabela DynamoDB `books` com chave composta (autor como partition key e gênero como sort key)
  - Bucket S3 `BookStorageBucket` para armazenamento dos arquivos CSV
  - Configurações de segurança e criptografia para ambos os recursos

- **`aws/lambda/booksEtl.yaml`**: Template para criação apenas da função Lambda de ETL (Extract, Transform, Load) dos livros. Requer que DynamoDB e S3 já existam.

#### Scripts e Recursos

- **`aws/deploy.sh`**: Script bash automatizado para fazer o deploy completo da aplicação. Veja a seção [Deploy Rápido](#-deploy-rápido) acima.

- **`aws/literatura_brasileira.csv`**: Arquivo CSV de exemplo contendo dados de livros da literatura brasileira para processamento pela função Lambda.

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

### Configuração do Projeto

O projeto utiliza Maven para gerenciamento de dependências e build. Principais configurações:

- **Java 21**: Runtime do Lambda
- **Maven Shade Plugin**: Configurado para gerar uber JAR com todas as dependências incluídas
- **AWS SDK 2.34.0**: Para integração com serviços AWS (Lambda, S3, DynamoDB)
- **Lombok**: Para redução de boilerplate
- **MapStruct**: Para mapeamento entre objetos de domínio e entidades
- **OpenCSV**: Para parsing de arquivos CSV
- **Jakarta CDI**: Para injeção de dependências

O arquivo `pom.xml` contém todas as dependências e configurações necessárias. O plugin `maven-shade-plugin` está configurado para criar automaticamente um uber JAR durante o build.

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
