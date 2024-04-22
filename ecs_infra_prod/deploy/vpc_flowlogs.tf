data "aws_caller_identity" "current" {
}
resource "aws_flow_log" "flow_log" {
  log_destination      = aws_s3_bucket.flowlog_bucket.arn
  log_destination_type = "s3"
  #iam_role_arn         = aws_iam_role.flowlog_role.arn
  vpc_id       = aws_vpc.main.id
  traffic_type = "ALL"

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-flowlogs" },
  )
}

resource "aws_iam_role" "flowlog_role" {
  name = "${local.prefix}-FlowLog-role"

  assume_role_policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Sid": "",
      "Effect": "Allow",
      "Principal": {
        "Service": "vpc-flow-logs.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF
  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-flowlogs-role" },
  )
}

resource "aws_iam_role_policy" "flowlog_policy" {
  name = "${local.prefix}-FlowLog-Policy"
  role = aws_iam_role.flowlog_role.id

  policy = <<EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Action": [
        "logs:CreateLogDelivery",
        "logs:DeleteLogDelivery"
      ],
      "Effect": "Allow",
      "Resource": "*"
    }
  ]
}
EOF
}

resource "aws_s3_bucket" "flowlog_bucket" {
  bucket = "${local.prefix}-${data.aws_caller_identity.current.account_id}-flowlog"

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-${data.aws_caller_identity.current.account_id}-flowlog" },
  )
}

resource "aws_s3_bucket_versioning" "flowlog_bucket" {
  bucket = aws_s3_bucket.flowlog_bucket.id
  versioning_configuration {
    status = "Enabled"
  }
}

resource "aws_s3_bucket_ownership_controls" "flowlog_bucket" {
  bucket = aws_s3_bucket.flowlog_bucket.id
  rule {
    object_ownership = "BucketOwnerPreferred"
  }
}

resource "aws_s3_bucket_acl" "flowlog_bucket" {
  depends_on = [aws_s3_bucket_ownership_controls.flowlog_bucket]

  bucket = aws_s3_bucket.flowlog_bucket.id
  acl    = "private"
}

resource "aws_s3_bucket_server_side_encryption_configuration" "flowlog_bucket" {
  bucket = aws_s3_bucket.flowlog_bucket.id

  rule {
    apply_server_side_encryption_by_default {
      sse_algorithm = "AES256"
    }
  }
}

resource "aws_s3_bucket_accelerate_configuration" "flowlog_bucket" {
  bucket = aws_s3_bucket.flowlog_bucket.id
  status = "Enabled"
}

resource "aws_s3_bucket_lifecycle_configuration" "flowlog_bucket" {
  bucket = aws_s3_bucket.flowlog_bucket.id

  rule {
    id     = "flowlog-expiration"
    status = "Enabled"

    filter {
      prefix = "VPCIOLogs/"
    }

    transition {
      days          = var.infrequent_transition_days
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = var.glacier_transition_days
      storage_class = "GLACIER"
    }

    expiration {
      days = var.expiration_days
    }
  }
}

resource "aws_s3_bucket_logging" "flowlog_bucket" {
  bucket = aws_s3_bucket.flowlog_bucket.id

  target_bucket = aws_s3_bucket.logging.id
  target_prefix = "s3-logging/logs/flowlog_bucket/"
}

resource "aws_s3_bucket_policy" "flowlog_delivery_policy" {
  bucket = aws_s3_bucket.flowlog_bucket.id

  policy = <<POLICY
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "AWSLogDeliveryWrite",
            "Effect": "Allow",
            "Principal": {"Service": "delivery.logs.amazonaws.com"},
            "Action": "s3:PutObject",
            "Resource": "${aws_s3_bucket.flowlog_bucket.arn}/VPCIOLogs/${data.aws_caller_identity.current.account_id}/*",
            "Condition": {"StringEquals": {"s3:x-amz-acl": "bucket-owner-full-control"}}
        },
        {
            "Sid": "AWSLogDeliveryAclCheck",
            "Effect": "Allow",
            "Principal": {"Service": "delivery.logs.amazonaws.com"},
            "Action": "s3:GetBucketAcl",
            "Resource": "${aws_s3_bucket.flowlog_bucket.arn}"
        }
    ]
}
POLICY

}

resource "aws_s3_bucket_logging" "vpc-flowlogs" {
  bucket = aws_s3_bucket.keystore-jks.id

  target_bucket = aws_s3_bucket.logging.id
  target_prefix = "s3-logging/logs/keystore-jks/"
}

/*
# keep this 180 days after changing delivery of flowlogs to s3
resource "aws_cloudwatch_log_group" "flowlog_log_group" {
  count = var.create_vpc && var.enable_flow_log ? 1 : 0
  name  = var.vpc_id

  tags = merge(
    var.common_tags,
    {
      "Name" = var.project_name
    },
  )

  retention_in_days = var.expiration_days
}
*/

# VPC flow log
output "vpc_flow_log_id" {
  description = "The ID of the Flow Log resource"
  value       = concat(aws_flow_log.flow_log.*.id, [""])[0]
}

output "vpc_flow_log_destination_arn" {
  description = "The ARN of the destination for VPC Flow Logs"
  value       = aws_s3_bucket.flowlog_bucket.arn
}

output "vpc_flow_log_destination_type" {
  description = "The type of the destination for VPC Flow Logs"
  value       = aws_flow_log.flow_log.log_destination_type
}

output "vpc_flow_log_cloudwatch_iam_role_arn" {
  description = "The ARN of the IAM role used when pushing logs to Cloudwatch log group"
  value       = aws_iam_role.flowlog_role.arn
}
