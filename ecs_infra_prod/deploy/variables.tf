variable "STAGING_AWS_ACCOUNT_ID" {
  default = null
}
variable "STAGING_AWS_SECRET_ACCESS_KEY" {
  default = null
}
variable "STAGING_AWS_SESSION_TOKEN" {
  default = null
}

variable "project_name" {}
variable "owner_name" {}
variable "expiry_date" {}
variable "aws_region" {
  default = "eu-central-1"
}
variable "dynamodb_tf_statelock" {}

variable "vpc_cidr_block" {
  default = "10.0.0.0/16"
}

variable "ssl_policy" {
  default = "ELBSecurityPolicy-2016-08"
}

//Route53 variables
variable "public_hosted_zone_id" {}
variable "route53_record_name" {}
variable "domain_name" {}
variable "prod_alb_certificate" {}

//Subnet variables
variable "public_subnet_ipv4_cidrs" {
  default = ["10.0.0.0/18", "10.0.64.0/18"]
}
variable "private_subnet_ipv4_cidrs" {
  default = ["10.0.128.0/18", "10.0.192.0/18"]
}

//bastion host variables
variable "bastion_key_name" {
  default = null
}

//flowlogs

variable "infrequent_transition_days" {
  default = "30"
}

variable "glacier_transition_days" {
  default = "90"
}

variable "expiration_days" {
  default = "180"
}

# //Database variables
variable "db_multi_az" {
  default = false
}
variable "db_password" {}
variable "db_username" {}
variable "db_engine" {
  default = "postgres"
}
variable "db_engine_version" {
  default = 12.8
}
variable "db_allocated_storage" {
  default = 30
}
variable "db_instance_class" {
  default = "db.t2.small"
}
variable "nextwork_db_snapshot" {
  type    = string
  default = null
}



variable "docdb_engine_version" {
  default = "5.0.0"
}
variable "docdb_port" {
  default = "27017"
}
variable "docdb_instance_count" {
  default = "2"
}
variable "docdb_instance_class" {
  default = "db.t4g.medium"
}
variable "docdb_username" {
}
variable "docdb_password" {
}
variable "nextwork_docdb_snapshot" {
  type    = string
  default = null
}
variable "docdb_enabled_cloudwatch_logs_exports" {
  default = ["profiler"]
}

