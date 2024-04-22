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
route53_record_name   = "app"
public_hosted_zone_id = "Z095375819F40YPJILAR6"
domain_name           = "nextworktool.siemens.cloud"
prod_alb_certificate  = "arn:aws:acm:eu-central-1:469624554140:certificate/4cea530a-d67f-4734-ba18-189d872f310d"
#old value arn:aws:acm:eu-central-1:469624554140:certificate/fa58e7ff-da55-4988-9fd7-8f174b1049be##

##########  Database details  ##########
db_username = "nextworkdb"
//db_password       = "test123$"
db_instance_class    = "db.t2.small"
db_multi_az          = true
db_engine_version    = 12.14
docdb_instance_class = "db.r6g.xlarge"

##########  Document Database details  ##########
docdb_username = "nextworkdb"

###########  Snapshot Database details  ##########
nextwork_db_snapshot    = "rds-prod-db-snap-2023-11-24-01-05-key-self"
nextwork_docdb_snapshot = ""