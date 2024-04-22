provider "aws" {
  region = var.aws_region
}

provider "aws" {
  alias      = "staging"
  access_key = var.STAGING_AWS_ACCOUNT_ID == null ? "" : var.STAGING_AWS_ACCOUNT_ID
  secret_key = var.STAGING_AWS_SECRET_ACCESS_KEY == null ? "" : var.STAGING_AWS_SECRET_ACCESS_KEY
  token      = var.STAGING_AWS_SESSION_TOKEN == null ? "" : var.STAGING_AWS_SESSION_TOKEN
}

terraform {
  backend "s3" {
    encrypt = true
  }
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = ">= 5.11"
    }
    tls = {
      source  = "hashicorp/tls"
      version = "4.0.4"
    }
  }
}

locals {
  prefix = "${lower(var.project_name)}-${lower(terraform.workspace)}"
  common_tags = {
    Environment = terraform.workspace
    Project     = var.project_name
    Owner       = var.owner_name
    ExpiryDate  = var.expiry_date
  }
}

data "aws_region" "current" {}
data "aws_availability_zones" "available" {
  state = "available"
}

output "common_tags" {
  value = local.common_tags
}
output "prefix" {
  value = local.prefix
}

output "dynamodb_tf_statelock" {
  value = var.dynamodb_tf_statelock
}

