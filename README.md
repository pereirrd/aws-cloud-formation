[Português](README.pt-br.md) | [Español](README.es.md)

# aws-cloud-formation

Project demonstrating the use of AWS CloudFormation service.

## 🚀 Quick Deploy

To deploy the application to AWS, use the automated script:

```bash
# Complete deployment (DynamoDB + S3 + Lambda)
./aws/deploy.sh complete

# Deploy Lambda only (requires existing DynamoDB and S3)
./aws/deploy.sh

# Show help
./aws/deploy.sh help
```

The script automatically:
- ✅ Compiles the project with Maven
- ✅ Generates the uber JAR with all dependencies
- ✅ Uploads the JAR to S3
- ✅ Validates the CloudFormation template
- ✅ Creates or updates the stack with necessary permissions

**Prerequisites:**
- AWS CLI configured with valid credentials
- Maven installed
- S3 bucket `lambdaCode` created (the script creates it automatically if it doesn't exist)

**AWS Region:** All resources will be created in the **us-east-1** (N. Virginia) region by default. This configuration is defined in the deploy script and CloudFormation templates.

For more details about the deployment process, see the [`DEPLOY.md`](DEPLOY.md) file.

## Project Structure

### `aws/` Directory

Contains CloudFormation scripts (YAML) and the automated deployment script for creating and managing AWS resources:

#### CloudFormation Templates

- **`aws/booksInfra-complete.yaml`**: Complete template that creates all infrastructure in a single stack:
  - DynamoDB table `books` with composite key (author as partition key and genre as sort key)
  - S3 bucket `BookStorageBucket` for storing CSV files
  - Lambda function `booksEtl` with permissions to access DynamoDB and S3
  - IAM Role with all necessary permissions
  - CloudWatch Log Group
  - Security and encryption settings

- **`aws/booksInfra-no-lambda.yaml`**: Template that creates only the basic infrastructure (without Lambda):
  - DynamoDB table `books` with composite key (author as partition key and genre as sort key)
  - S3 bucket `BookStorageBucket` for storing CSV files
  - Security and encryption settings for both resources

- **`aws/lambda/booksEtl.yaml`**: Template for creating only the Lambda ETL (Extract, Transform, Load) function for books. Requires DynamoDB and S3 to already exist.

#### Scripts and Resources

- **`aws/deploy.sh`**: Automated bash script to deploy the complete application. See the [Quick Deploy](#-quick-deploy) section above.

- **`aws/literatura_brasileira.csv`**: Example CSV file containing Brazilian literature book data for processing by the Lambda function.

### `src/` Directory

Contains the Java source code of the AWS Lambda function created by the CloudFormation scripts.

#### `src/main/java/com/books/`
Java code structure organized in packages:

- **`Main.java`**: Main class with `main` method for application initialization.
- **`application/`**: Application layer:
  - `BookApplication.java`: Main class that orchestrates the ETL processing flow for books.
- **`core/`**: Configuration and infrastructure classes:
  - `Configuration.java`: Loads and manages application configuration from the `application.properties` file.
  - `DynamoDbClientFactory.java`: Factory for creating the DynamoDB Enhanced Client.
- **`domain/`**: Domain layer:
  - **`book/`**: Book models and mappers:
    - `Book.java`: Domain model representing a book with attributes: title, author, genre, and period.
    - `BookMapper.java`: MapStruct interface for conversion between `Book` (domain) and `BookEntity` (persistence).
  - **`csv/`**: CSV processing services:
    - `CsvService.java`: Service responsible for parsing CSV files into `Book` objects.
  - **`exception/`**: Custom exceptions:
    - `FileNotFoundException.java`: Exception thrown when a file is not found in S3.
    - `ProcessingException.java`: Generic exception for errors during processing.
- **`repository/`**: Data access layer:
  - **`dynamo/`**: DynamoDB repository and entities:
    - `BookRepository.java`: Repository for CRUD operations with books in DynamoDB (save, findByAutorAndGenero, delete).
    - `entity/BookEntity.java`: Entity mapped from the DynamoDB table with composite key (author as partition key and genre as sort key).
  - **`s3/`**: S3 repository:
    - `S3Bucket.java`: Class for reading files from the S3 bucket, supporting reading as String or byte array.

#### `src/main/resources/`
Application resources:
- **`application.properties`**: Configuration file containing:
  - `s3.url`: URL of the S3 bucket where CSV files are stored.
  - `dynamodb.table.books`: Name of the DynamoDB table where books will be persisted.

#### `src/test/`
Unit and integration tests for the Java code.

### Project Configuration

The project uses Maven for dependency management and build. Main configurations:

- **Java 21**: Lambda runtime
- **Maven Shade Plugin**: Configured to generate uber JAR with all dependencies included
- **AWS SDK 2.34.0**: For integration with AWS services (Lambda, S3, DynamoDB)
- **Lombok**: For boilerplate reduction
- **MapStruct**: For mapping between domain objects and entities
- **OpenCSV**: For CSV file parsing
- **Jakarta CDI**: For dependency injection

The `pom.xml` file contains all necessary dependencies and configurations. The `maven-shade-plugin` is configured to automatically create an uber JAR during the build.

## Lambda Function Operation

The Lambda function implements an ETL (Extract, Transform, Load) process to process CSV files of books stored in S3 and persist the data in DynamoDB.

### Processing Flow

The processing flow is orchestrated by the `BookApplication` class through the `processCsvFile(String fileName)` method:

1. **Extract (Extraction)**:
   - The function receives a CSV file name as a parameter.
   - `S3Bucket` reads the file from the configured S3 bucket, returning the content as a byte array.
   - The S3 URL is resolved from the configuration (`s3.url`), extracting the bucket and prefix/path.
   - If the file is not found, a `FileNotFoundException` is thrown.

2. **Transform (Transformation)**:
   - `CsvService` parses the CSV content (byte array) into a list of `Book` objects.
   - Uses the OpenCSV library with `@CsvBindByName` annotations to map CSV columns (titulo, autor, genero, periodo) to `Book` model attributes.
   - Parsing ignores blank lines and whitespace at the beginning of lines.

3. **Load (Loading)**:
   - Each `Book` object is converted to `BookEntity` using `BookMapper` (MapStruct).
   - Books are saved to DynamoDB through `BookRepository`, using streams and lambdas to process each item.
   - The DynamoDB table uses a composite key:
     - **Partition Key**: `autor` (book author)
     - **Sort Key**: `genero` (book genre)
   - Each book is persisted individually, with detailed logs of success or error.

### Error Handling

The code implements error handling at three levels:

- **`FileNotFoundException`**: When the file is not found in S3.
- **`ProcessingException`**: For errors during CSV parsing or persistence in DynamoDB.
- **Unexpected errors**: Captured and encapsulated in `ProcessingException` with descriptive messages.

All errors are logged using SLF4J before being rethrown.

### Configuration and Dependency Injection

The application uses Jakarta CDI (Contexts and Dependency Injection) for dependency injection:

- Classes marked with `@Singleton` are managed as singletons.
- Dependencies are injected via constructors using Lombok's `@RequiredArgsConstructor`.
- `Configuration` loads properties from the `application.properties` file on initialization.
- `DynamoDbClientFactory` creates the DynamoDB Enhanced Client necessary for database operations.

### Architecture

The application follows a layered architecture:

- **Application Layer**: `BookApplication` - orchestrates the business flow.
- **Domain Layer**: Models (`Book`), services (`CsvService`), and domain-specific exceptions.
- **Repository Layer**: Access to external data (S3 and DynamoDB).
- **Core Layer**: Configurations and factories for AWS infrastructure.

This separation facilitates maintenance, testability, and code evolution.
