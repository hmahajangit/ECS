provider "aws" {
  region = var.aws_region
}

data "aws_region" "current" {}

/*For initializing remote state storage can be called with variables
terraform init -backend-config='bucket=bucketname'*/
terraform {
  backend "s3" {
    encrypt = true
  }
}

/*For initializing, the state of the core terraform and get the required variables*/
data "terraform_remote_state" "core_tfstate" {
  backend = "s3"
  config = {
    bucket = var.remote_state_bucket
    key    = "${var.PROJECT_NAME}/${terraform.workspace}/${var.remote_state_core_path}" #####Workaround: passing an absolute path for remote state file 
    region = data.aws_region.current.name
    #workspace_key_prefix = var.workspace_key_prefix  ####TODO: Bug reported w.r.t workspace_key_prefix https://github.com/hashicorp/terraform/issues/27499
  }
}
