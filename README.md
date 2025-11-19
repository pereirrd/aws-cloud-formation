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

- **`Main.java`**: Classe principal da aplicação Lambda.
- **`core/`**: Classes de configuração e constantes:
  - `Configuration.java`: Configurações da aplicação.
  - `constants/DynamoTableConstants.java`: Constantes relacionadas à tabela DynamoDB.
- **`repository/`**: Camada de acesso a dados:
  - **`dynamo/`**: Repositório e entidades DynamoDB:
    - `BookRepository.java`: Repositório para operações com livros no DynamoDB.
    - `entity/BookEntity.java`: Entidade mapeada da tabela DynamoDB.
  - **`s3/`**: Repositório S3:
    - `S3Bucket.java`: Classe para leitura de arquivos do bucket S3.

#### `src/main/resources/`
Recursos da aplicação (configurações, arquivos estáticos, etc.).

#### `src/test/`
Testes unitários e de integração do código Java.
