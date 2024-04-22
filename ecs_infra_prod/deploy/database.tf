resource "aws_db_subnet_group" "main" {
  name       = "${local.prefix}-main"
  subnet_ids = tolist(aws_subnet.private[*].id)

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-main" },
  )
}

resource "aws_security_group" "rds" {
  description = "Allow access to the RDS database instance"
  name        = "${local.prefix}-rds-inbound-access"
  vpc_id      = aws_vpc.main.id

  ingress {
    protocol  = "tcp"
    from_port = 5432
    to_port   = 5432

    security_groups = [
      aws_security_group.bastion.id,
      aws_security_group.ecs_service.id,
    ]
  }

  tags = local.common_tags
}

resource "aws_db_instance" "primary" {
  identifier                = "${local.prefix}-db"
  db_name                   = "${lower(var.project_name)}db"
  allocated_storage         = var.db_allocated_storage
  storage_type              = "gp2"
  engine                    = var.db_engine
  engine_version            = var.db_engine_version
  instance_class            = var.db_instance_class
  db_subnet_group_name      = aws_db_subnet_group.main.name
  snapshot_identifier       = var.nextwork_db_snapshot == "null" ? "" : var.nextwork_db_snapshot
  password                  = var.db_password
  username                  = var.db_username
  multi_az                  = var.db_multi_az
  backup_retention_period   = 7
  backup_window             = "08:00-10:00"
  storage_encrypted         = true
  skip_final_snapshot       = false
  final_snapshot_identifier = "${local.prefix}db-${formatdate("DDMMMYYYY-hh-mm-ZZZ", timestamp())}"
  copy_tags_to_snapshot     = true
  vpc_security_group_ids    = [aws_security_group.rds.id]

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-primary" },
  )
}

resource "aws_ssm_parameter" "database_password_parameter" {
  name        = "${local.prefix}_database_password"
  description = "${local.prefix} database password"
  type        = "SecureString"
  value       = var.db_password

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}_database_password" },
  )
}

#Database Output
output "primary_database_endpoint" {
  value = aws_db_instance.primary.endpoint
}
output "primary_database_name" {
  value = aws_db_instance.primary.db_name
}

output "primary_database_username" {
  value = aws_db_instance.primary.username
}

output "database_password_ssm_parameter_arn" {
  value = aws_ssm_parameter.database_password_parameter.arn
}
output "database_password_ssm_parameter_name" {
  value = aws_ssm_parameter.database_password_parameter.name
}
