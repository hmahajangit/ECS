########## COMMON Details ##########
owner_name            = "ancy.crasta@siemens.com"
expiry_date           = "30/3/2023"
tf_statelock_dynamodb = "se-dynamodb-tf-statelock"

##########  VPC details     ##########
vpc_cidr_block = "10.0.0.0/16"

########## ALB details  ###########
ssl_policy = "ELBSecurityPolicy-FS-1-2-2019-08"

##########  Subnet variables    ##########
public_subnet_ipv4_cidrs  = ["10.0.0.0/18", "10.0.64.0/18"]
private_subnet_ipv4_cidrs = ["10.0.128.0/18", "10.0.192.0/18"]

##########  Route53 details  ##########
route53_record_name   = "app.uat"
public_hosted_zone_id = "Z095375819F40YPJILAR6"
domain_name           = "nextworktool.siemens.cloud"
prod_alb_certificate  = ""

##########  Database details  ##########
db_username = "nextworkdb"
//db_password       = "test123$"
db_instance_class    = "db.t2.small"
db_multi_az          = false
db_engine_version    = 12.14
docdb_instance_class = "db.r6g.xlarge"
##########  Document Database details  ##########
docdb_username = "nextworkdb"
##[ci skip]


### DB enable log export ####
docdb_enabled_cloudwatch_logs_exports = ["audit", "profiler"]

###########  Snapshot Database details  ##########
nextwork_docdb_snapshot = "manual-nextwork-uat-docdb-2023-11-17-22-42"
nextwork_db_snapshot    = "nextwork-uatdb-29oct2023-18-01-utc"