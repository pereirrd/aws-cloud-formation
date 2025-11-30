# Guia de Deploy do Lambda booksEtl

Este guia explica como fazer o deploy da função Lambda usando o template CloudFormation.

## Pré-requisitos

1. **AWS CLI configurado** com credenciais válidas
2. **Maven** instalado (para compilar o projeto Java)
3. **Bucket S3** chamado `lambdaCode` criado na sua conta AWS
4. **Permissões AWS** adequadas para:
   - Criar/atualizar stacks CloudFormation
   - Criar recursos Lambda, IAM e CloudWatch Logs
   - Fazer upload de arquivos no S3

## Passo a Passo

### 1. Compilar o Projeto Java

Compile o projeto para gerar o uber JAR com todas as dependências:

```bash
# Compilar o projeto
mvn clean package

# O JAR será gerado em: target/aws-cloud-formation-1.0.0-SNAPSHOT.jar
```

**Nota:** O `pom.xml` já está configurado com o `maven-shade-plugin`, então o JAR gerado incluirá automaticamente todas as dependências necessárias (AWS SDK, OpenCSV, Jakarta Inject, MapStruct, etc.) para execução no Lambda.

### 2. Renomear o JAR (opcional)

Se necessário, renomeie o JAR para corresponder ao nome esperado pelo template:

```bash
cp target/aws-cloud-formation-1.0.0-SNAPSHOT.jar booksEtl.jar
```

### 3. Fazer Upload do JAR para o S3

Faça upload do arquivo JAR para o bucket S3 `lambdaCode`:

```bash
aws s3 cp booksEtl.jar s3://lambdaCode/booksEtl.jar
```

**Importante:** Certifique-se de que o bucket `lambdaCode` existe. Se não existir, crie-o primeiro:

```bash
aws s3 mb s3://lambdaCode --region us-east-1
```

### 4. Validar o Template CloudFormation (opcional)

Antes de fazer o deploy, valide o template:

```bash
# Para o template apenas do Lambda
aws cloudformation validate-template \
  --template-body file://aws/lambda/booksEtl.yaml

# Para o template completo (DynamoDB + S3 + Lambda)
aws cloudformation validate-template \
  --template-body file://aws/booksInfra-complete.yaml
```

### 5. Criar o Stack CloudFormation

Você pode escolher entre dois templates:

#### Opção A: Template apenas do Lambda (`booksEtl.yaml`)

Crie o stack apenas com a função Lambda (requer que DynamoDB e S3 já existam):

```bash
aws cloudformation create-stack \
  --stack-name books-etl-lambda \
  --template-body file://aws/lambda/booksEtl.yaml \
  --capabilities CAPABILITY_NAMED_IAM
```

#### Opção B: Template completo (`booksInfra-complete.yaml`)

Crie o stack completo com DynamoDB, S3 e Lambda em um único template:

```bash
aws cloudformation create-stack \
  --stack-name books-infra-complete \
  --template-body file://aws/booksInfra-complete.yaml \
  --capabilities CAPABILITY_NAMED_IAM
```

**Explicação dos parâmetros:**
- `--stack-name`: Nome do stack CloudFormation
- `--template-body`: Caminho para o arquivo YAML do template
- `--capabilities CAPABILITY_NAMED_IAM`: **OBRIGATÓRIO** - Necessário porque ambos os templates criam recursos IAM (Role). Sem esta flag, o deploy falhará.

### 6. Verificar o Status do Deploy

Acompanhe o progresso da criação do stack:

```bash
aws cloudformation describe-stacks --stack-name books-etl-lambda
```

Ou acompanhe em tempo real:

```bash
aws cloudformation wait stack-create-complete --stack-name books-etl-lambda
```

### 7. Verificar os Recursos Criados

Após o deploy, verifique se a função Lambda foi criada:

```bash
aws lambda get-function --function-name booksEtl
```

## Atualizar o Lambda (Re-deploy)

Quando você fizer alterações no código e precisar atualizar o Lambda:

### 1. Recompilar e fazer upload do novo JAR

```bash
mvn clean package
aws s3 cp booksEtl.jar s3://lambdaCode/booksEtl.jar
```

### 2. Atualizar o Stack

```bash
# Para o template apenas do Lambda
aws cloudformation update-stack \
  --stack-name books-etl-lambda \
  --template-body file://aws/lambda/booksEtl.yaml \
  --capabilities CAPABILITY_NAMED_IAM

# Para o template completo
aws cloudformation update-stack \
  --stack-name books-infra-complete \
  --template-body file://aws/booksInfra-complete.yaml \
  --capabilities CAPABILITY_NAMED_IAM
```

**Nota:** O CloudFormation detectará automaticamente a mudança no arquivo S3 e atualizará a função Lambda.

**⚠️ IMPORTANTE:** Sempre inclua a flag `--capabilities CAPABILITY_NAMED_IAM` ao criar ou atualizar stacks que contenham recursos IAM (Roles, Policies, etc.). Sem esta flag, o CloudFormation rejeitará o deploy por segurança.

## Deletar o Stack

Para remover todos os recursos criados:

```bash
aws cloudformation delete-stack --stack-name books-etl-lambda
```

## Troubleshooting

### Erro: Bucket não encontrado
- Certifique-se de que o bucket `lambdaCode` existe
- Verifique se você tem permissões para acessar o bucket

### Erro: Permissões insuficientes
- Verifique suas credenciais AWS: `aws sts get-caller-identity`
- Certifique-se de ter as políticas IAM necessárias

### Erro: Handler não encontrado
- Verifique se a classe `com.example.BooksEtlHandler` existe no código
- Confirme que o método `handleRequest` está implementado corretamente
- Verifique se o JAR contém todas as classes necessárias

### Ver logs do Lambda

```bash
aws logs tail /aws/lambda/booksEtl --follow
```

### Erro: ClassNotFoundException ou NoClassDefFoundError

Se você encontrar erros de classe não encontrada ao executar o Lambda, significa que o uber JAR não foi criado corretamente. Verifique:

1. O JAR foi gerado com todas as dependências? Verifique o tamanho do arquivo (deve ser maior que alguns MB devido às dependências AWS SDK)
2. Você está usando o JAR correto para upload no S3?
3. O comando `mvn clean package` foi executado com sucesso?

## Script de Deploy Automatizado

O projeto já inclui um script de deploy automatizado (`aws/deploy.sh`) que faz todo o processo:

### Uso do Script

```bash
# Deploy apenas do Lambda (requer DynamoDB e S3 existentes)
./aws/deploy.sh

# Deploy completo (DynamoDB + S3 + Lambda)
./aws/deploy.sh complete

# Ver ajuda
./aws/deploy.sh help
```

O script automaticamente:
- ✅ Compila o projeto com Maven
- ✅ Gera o uber JAR com todas as dependências
- ✅ Faz upload do JAR para o S3
- ✅ Valida o template CloudFormation
- ✅ Cria ou atualiza o stack com a flag `--capabilities CAPABILITY_NAMED_IAM`
- ✅ Exibe informações da função Lambda após o deploy

**Nota:** O script já inclui a flag `--capabilities CAPABILITY_NAMED_IAM` automaticamente, então você não precisa se preocupar com isso.

