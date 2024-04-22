resource "aws_ecs_cluster" "main" {
  name = "${local.prefix}-cluster"

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-cluster" },
  )
}

resource "aws_security_group" "ecs_service" {
  description = "Access for the ECS Service"
  name        = "${local.prefix}-ecs-service"
  vpc_id      = aws_vpc.main.id

  egress {
    from_port   = 443
    to_port     = 443
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  #egress {
  #  from_port   = 80
  #  to_port     = 80
  #  protocol    = "tcp"
  #  cidr_blocks = ["0.0.0.0/0"]
  #}

  egress {
    from_port   = 27017
    to_port     = 27017
    protocol    = "tcp"
    cidr_blocks = tolist(aws_subnet.private[*].cidr_block)
  }
  egress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = tolist(aws_subnet.private[*].cidr_block)
  }

  ingress {
    from_port = 8000
    to_port   = 9000
    protocol  = "tcp"
    security_groups = [
      aws_security_group.lb.id
    ]
  }
  ingress {
    from_port = 443
    to_port   = 443
    protocol  = "tcp"
    security_groups = [
      aws_security_group.lb.id
    ]
  }
  #  ingress {
  #    from_port = 80
  #    to_port   = 80
  #    protocol  = "tcp"
  #    security_groups = [
  #      aws_security_group.lb.id
  #    ]
  #  }

  tags = local.common_tags
}


#####ECS Cluster
output "ecs_cluster_id" {
  value = aws_ecs_cluster.main.id
}
output "ecs_cluster_name" {
  value = aws_ecs_cluster.main.name
}
output "ecs_securitygroup_id" {
  value = aws_security_group.ecs_service.id
}