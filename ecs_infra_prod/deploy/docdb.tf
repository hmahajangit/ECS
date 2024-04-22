# resource "aws_db_subnet_group" "main" {
#   name       = "${local.prefix}-main"
#   subnet_ids = tolist(aws_subnet.private[*].id)

#   tags = merge(
#     local.common_tags,
#     { "Name" = "${local.prefix}-main" },
#   )
# }

resource "aws_security_group" "docdb" {
  description = "Allow access to the Document database instance"
  name        = "${local.prefix}-docdb-inbound-access"
  vpc_id      = aws_vpc.main.id

  ingress {
    protocol  = "tcp"
    from_port = var.docdb_port
    to_port   = var.docdb_port

    security_groups = [
      aws_security_group.bastion.id,
      aws_security_group.ecs_service.id,
    ]
  }

  tags = local.common_tags
}


resource "aws_docdb_cluster_instance" "cluster_instances" {
  count              = var.docdb_instance_count
  identifier         = "${local.prefix}-docdbinstance-${count.index}"
  cluster_identifier = aws_docdb_cluster.docdb.id
  instance_class     = var.docdb_instance_class

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-docdbinstance-${count.index}" },
  )
}

resource "aws_docdb_cluster" "docdb" {
  cluster_identifier              = "${local.prefix}-docdb"
  engine                          = "docdb"
  engine_version                  = var.docdb_engine_version
  master_username                 = var.docdb_username
  master_password                 = var.docdb_password #https://github.com/hashicorp/terraform-provider-aws/issues/13472
  port                            = var.docdb_port
  backup_retention_period         = 30
  preferred_backup_window         = "08:00-10:00"
  skip_final_snapshot             = false
  storage_encrypted               = true
  snapshot_identifier             = var.nextwork_docdb_snapshot == "null" ? "" : var.nextwork_docdb_snapshot
  db_subnet_group_name            = aws_db_subnet_group.main.name
  enabled_cloudwatch_logs_exports = var.docdb_enabled_cloudwatch_logs_exports
  final_snapshot_identifier       = "${local.prefix}-docdb-${formatdate("DDMMMYYYY-hh-mm-ZZZ", timestamp())}"
  vpc_security_group_ids          = [aws_security_group.docdb.id]
  kms_key_id                      = aws_kms_key.docdbkms.arn
  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-docdb" },
  )
}

# resource "aws_ssm_parameter" "database_password_parameter" {
#   name        = "${local.prefix}_database_password"
#   description = "${local.prefix} database password"
#   type        = "SecureString"
#   value       = var.docdb_password

#   tags = merge(
#     local.common_tags,
#     { "Name" = "${local.prefix}_database_password" },
#   )
# }


output "docdb_database_endpoint" {
  value = aws_docdb_cluster.docdb.endpoint
}
output "docdb_database_name" {
  value = aws_docdb_cluster.docdb.cluster_identifier
}
output "docdb_database_username" {
  value = aws_docdb_cluster.docdb.master_username
}
output "docdb_database_password" {
  value     = aws_docdb_cluster.docdb.master_password
  sensitive = true
}
# output "database_password_ssm_parameter_arn" {
#   value = aws_ssm_parameter.database_password_parameter.arn
# }
# output "database_password_ssm_parameter_name" {
#   value = aws_ssm_parameter.database_password_parameter.name
# }
