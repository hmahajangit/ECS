#!/bin/bash

export s3_tfstate=$PROJECT_NAME-$AWS_ACCOUNT_ID-tfstate
export TF_VAR_dynamodb_tf_statelock=se-dynamodb-tf-statelock
# export role_arn=arn:aws:iam::$NEW_AWS_ACCOUNT_ID:role/$AWS_ROLE

bucket_check=$(aws s3api head-bucket --bucket "$s3_tfstate" 2>&1) ||  echo "S3 Bucket exists"
if [[ -z "$bucket_check" ]]; then
  echo "S3 Bucket ${s3_tfstate} exists"
  echo "updating bucket accelerate-configuration property"

  # Check if Transfer Acceleration is enabled
  transfer_acceleration_status=$(aws s3api get-bucket-accelerate-configuration --bucket $s3_tfstate --query 'Status' --output text)

  if [ "$transfer_acceleration_status" == "Enabled" ]; then
    echo "Transfer Acceleration is already enabled."
  else
    echo "Transfer Acceleration is not enabled. Enabling..."
    aws s3api put-bucket-accelerate-configuration --bucket $s3_tfstate --accelerate-configuration Status=Enabled
    echo "Transfer Acceleration has been enabled."
  fi  
  # Check if Server Access Logging is enabled
  server_access_logging_status=$(aws s3api get-bucket-logging --bucket $s3_tfstate --query 'LoggingEnabled' --output text)

  if [ "$server_access_logging_status" == "True" ]; then
    echo "Server Access Logging is enabled for the bucket."
  else
    echo "Server Access Logging is going to be enabled for the bucket... ${s3_tfstate}"
    aws s3api put-object --bucket $s3_tfstate --key logs
    aws s3api put-bucket-logging --bucket $s3_tfstate --bucket-logging-status "{\"LoggingEnabled\":{\"TargetBucket\":\"${s3_tfstate}\",\"TargetPrefix\":\"logs/\"}}"
  fi 

elif [[ "$AWS_REGION" == "us-east-1" ]]; then
  echo "Creating s3 Bucket ${s3_tfstate}"
  aws s3api create-bucket --bucket ${s3_tfstate} --region ${AWS_REGION}
  aws s3api put-bucket-versioning --bucket ${s3_tfstate} --versioning-configuration Status=Enabled
  aws s3api put-bucket-encryption --bucket ${s3_tfstate} --server-side-encryption-configuration '{"Rules": [{"ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "AES256"}}]}'
  aws s3api put-bucket-accelerate-configuration --bucket ${s3_tfstate} --accelerate-configuration Status=Enabled
else
  echo "Creating s3 Bucket ${s3_tfstate}"
  aws s3api create-bucket --bucket ${s3_tfstate} --region ${AWS_REGION} --create-bucket-configuration LocationConstraint=${AWS_REGION} 
  aws s3api put-bucket-versioning --bucket ${s3_tfstate} --versioning-configuration Status=Enabled
  aws s3api put-bucket-encryption --bucket ${s3_tfstate} --server-side-encryption-configuration '{"Rules": [{"ApplyServerSideEncryptionByDefault": {"SSEAlgorithm": "AES256"}}]}'
  aws s3api put-bucket-accelerate-configuration --bucket ${s3_tfstate} --accelerate-configuration Status=Enabled
  aws s3api put-object --bucket $s3_tfstate --key logs
  aws s3api put-bucket-logging --bucket $s3_tfstate --bucket-logging-status "{\"LoggingEnabled\":{\"TargetBucket\":\"${s3_tfstate}\",\"TargetPrefix\":\"logs/\"}}"  
fi

aws ecr create-repository --repository-name $PROJECT_NAME-repo --region ${AWS_REGION} ||  echo "ECR repository already exists, skipped repository creation"
aws ecr put-image-tag-mutability --repository-name $PROJECT_NAME-repo --image-tag-mutability IMMUTABLE --region ${AWS_REGION}
aws ecr put-image-scanning-configuration --repository-name $PROJECT_NAME-repo --image-scanning-configuration scanOnPush=true --region ${AWS_REGION}
aws ecr set-repository-policy --repository-name $PROJECT_NAME-repo --region ${AWS_REGION} --policy-text file://ecr.json

aws dynamodb create-table \
    --table-name $TF_VAR_dynamodb_tf_statelock \
    --attribute-definitions \
        AttributeName=LockID,AttributeType=S \
    --key-schema AttributeName=LockID,KeyType=HASH \
    --provisioned-throughput ReadCapacityUnits=10,WriteCapacityUnits=10 --region ${AWS_REGION} || echo "SE dynamoDB table already exists, skipped dynamoDB table creation"

aws dynamodb update-continuous-backups \
    --table-name $TF_VAR_dynamodb_tf_statelock \
    --point-in-time-recovery-specification PointInTimeRecoveryEnabled=true \
    --region ${AWS_REGION} || echo "SE dynamoDB is on PITR & Backups"

encryption_status=$(aws dynamodb describe-table --table-name "$TF_VAR_dynamodb_tf_statelock" --query 'Table.SSEDescription.SSEType' --output text --region ${AWS_REGION} 2>/dev/null)

if [[ "$encryption_status" == "KMS" ]]; then
  echo "DynamoDB table is already encrypted with KMS. Skipping update."
else
  # Run the update-table command to enable encryption with KMS
  if aws dynamodb update-table --table-name "$TF_VAR_dynamodb_tf_statelock" --sse-specification Enabled=true,SSEType=KMS --region ${AWS_REGION}; then
    echo "DynamoDB table encryption with KMS enabled successfully."
  else
    echo "Failed to enable encryption with KMS for DynamoDB table."
  fi
fi