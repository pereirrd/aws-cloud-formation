#!/bin/bash
set -e

# Mudar para o diretório raiz do projeto (onde está o pom.xml)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$PROJECT_ROOT"

# Cores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configurações padrão
STACK_NAME="books-etl-lambda"
BUCKET_NAME="lambdaCode"
JAR_NAME="booksEtl.jar"
TEMPLATE_PATH="$PROJECT_ROOT/aws/lambda/booksEtl.yaml"
JAR_SOURCE="$PROJECT_ROOT/target/aws-cloud-formation-1.0.0-SNAPSHOT.jar"

# Permitir escolher o template via argumento
if [ "$1" == "complete" ]; then
    STACK_NAME="books-infra-complete"
    TEMPLATE_PATH="$PROJECT_ROOT/aws/booksInfra-complete.yaml"
elif [ "$1" == "help" ] || [ "$1" == "-h" ] || [ "$1" == "--help" ]; then
    echo "Uso: $0 [complete]"
    echo ""
    echo "  Sem argumentos: Deploy apenas do Lambda (requer DynamoDB e S3 existentes)"
    echo "  complete:       Deploy completo (DynamoDB + S3 + Lambda)"
    echo ""
    exit 0
fi

echo -e "${GREEN}=== Deploy do Lambda booksEtl ===${NC}"
if [ "$1" == "complete" ]; then
    echo -e "${YELLOW}Modo: Template completo (DynamoDB + S3 + Lambda)${NC}"
else
    echo -e "${YELLOW}Modo: Apenas Lambda${NC}"
fi
echo ""

# Verificar se o Maven está instalado
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Erro: Maven não está instalado${NC}"
    exit 1
fi

# Verificar se o AWS CLI está instalado
if ! command -v aws &> /dev/null; then
    echo -e "${RED}Erro: AWS CLI não está instalado${NC}"
    exit 1
fi

# Verificar credenciais AWS
echo -e "${YELLOW}Verificando credenciais AWS...${NC}"
if ! aws sts get-caller-identity &> /dev/null; then
    echo -e "${RED}Erro: Credenciais AWS não configuradas${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Credenciais AWS OK${NC}\n"

# Compilar o projeto
echo -e "${YELLOW}Compilando o projeto...${NC}"
mvn clean package
if [ $? -ne 0 ]; then
    echo -e "${RED}Erro ao compilar o projeto${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Compilação concluída${NC}\n"

# Verificar se o JAR foi gerado
if [ ! -f "$JAR_SOURCE" ]; then
    echo -e "${RED}Erro: JAR não encontrado em $JAR_SOURCE${NC}"
    exit 1
fi

# Renomear JAR
echo -e "${YELLOW}Preparando JAR...${NC}"
cp "$JAR_SOURCE" "$JAR_NAME"
echo -e "${GREEN}✓ JAR preparado: $JAR_NAME${NC}\n"

# Verificar se o bucket existe
echo -e "${YELLOW}Verificando bucket S3...${NC}"
if ! aws s3 ls "s3://$BUCKET_NAME" &> /dev/null; then
    echo -e "${YELLOW}Bucket não encontrado. Criando bucket $BUCKET_NAME...${NC}"
    REGION=$(aws configure get region || echo "us-east-1")
    aws s3 mb "s3://$BUCKET_NAME" --region "$REGION"
    if [ $? -ne 0 ]; then
        echo -e "${RED}Erro ao criar bucket${NC}"
        exit 1
    fi
    echo -e "${GREEN}✓ Bucket criado${NC}\n"
else
    echo -e "${GREEN}✓ Bucket encontrado${NC}\n"
fi

# Fazer upload para S3
echo -e "${YELLOW}Fazendo upload para S3...${NC}"
aws s3 cp "$JAR_NAME" "s3://$BUCKET_NAME/$JAR_NAME"
if [ $? -ne 0 ]; then
    echo -e "${RED}Erro ao fazer upload para S3${NC}"
    exit 1
fi
echo -e "${GREEN}✓ Upload concluído${NC}\n"

# Validar template
echo -e "${YELLOW}Validando template CloudFormation...${NC}"
if aws cloudformation validate-template --template-body "file://$TEMPLATE_PATH" &> /dev/null; then
    echo -e "${GREEN}✓ Template válido${NC}\n"
else
    echo -e "${RED}Erro: Template inválido${NC}"
    exit 1
fi

# Verificar se o stack já existe
echo -e "${YELLOW}Verificando stack existente...${NC}"
if aws cloudformation describe-stacks --stack-name "$STACK_NAME" &> /dev/null; then
    echo -e "${YELLOW}Stack já existe. Atualizando...${NC}"
    OPERATION="update"
else
    echo -e "${YELLOW}Stack não existe. Criando...${NC}"
    OPERATION="create"
fi

# Criar ou atualizar stack
echo -e "${YELLOW}Executando deploy do CloudFormation...${NC}"
aws cloudformation deploy \
    --template-file "$TEMPLATE_PATH" \
    --stack-name "$STACK_NAME" \
    --capabilities CAPABILITY_NAMED_IAM

if [ $? -ne 0 ]; then
    echo -e "${RED}Erro ao fazer deploy do CloudFormation${NC}"
    exit 1
fi

echo -e "\n${GREEN}=== Deploy concluído com sucesso! ===${NC}\n"

# Mostrar informações do Lambda
echo -e "${YELLOW}Informações da função Lambda:${NC}"
aws lambda get-function --function-name booksEtl --query 'Configuration.[FunctionName,FunctionArn,Runtime,LastModified]' --output table

echo -e "\n${GREEN}Para ver os logs do Lambda, execute:${NC}"
echo -e "aws logs tail /aws/lambda/booksEtl --follow"

