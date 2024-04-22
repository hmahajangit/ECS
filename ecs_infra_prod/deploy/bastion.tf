data "aws_ami" "amazon_linux" {
  most_recent = true
  filter {
    name   = "name"
    values = ["amzn2-ami-hvm-2.0.*-x86_64-gp2"]
  }
  owners = ["amazon"]
}

resource "aws_iam_role" "bastion_role" {
  count = terraform.workspace == "staging" || terraform.workspace == "uat" ? 1 : 0
  name  = "${local.prefix}-bastion-role"

  # Terraform's "jsonencode" function converts a
  # Terraform expression result to valid JSON syntax.
  assume_role_policy = jsonencode({
    Version = "2012-10-17"
    Statement = [
      {
        Action = "sts:AssumeRole"
        Effect = "Allow"
        Sid    = ""
        Principal = {
          Service = "ec2.amazonaws.com"
        }
      },
    ]
  })

  managed_policy_arns = ["arn:aws:iam::aws:policy/AmazonSSMManagedInstanceCore"]

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-bastion-role" },
  )
}

resource "aws_iam_instance_profile" "bastion_profile" {
  count = terraform.workspace == "staging" || terraform.workspace == "uat" ? 1 : 0
  name  = "${local.prefix}-bastion-profile"
  role  = aws_iam_role.bastion_role[0].name

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-bastion-role-profile" },
  )
}

/*
resource "aws_eip" "bastion" {
  count    = terraform.workspace == "staging" ? 1 : 0
  instance = aws_instance.bastion[0].id
  vpc      = true

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-bastion-eip" },
  )
}*/

resource "aws_instance" "bastion" {
  count         = terraform.workspace == "staging" || terraform.workspace == "uat" ? 1 : 0
  ami           = data.aws_ami.amazon_linux.id
  instance_type = "t2.micro"
  subnet_id     = aws_subnet.private[0].id

  iam_instance_profile = aws_iam_instance_profile.bastion_profile[0].name

  vpc_security_group_ids = [
    aws_security_group.bastion.id
  ]

  root_block_device {
    encrypted  = true
    kms_key_id = aws_kms_key.awsebs.arn
  }

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-bastion" },
  )
  volume_tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-bastion" },
  )
}

resource "aws_security_group" "bastion" {
  description = "Control bastion inbound and outbound access"
  name        = "${local.prefix}-bastion"
  vpc_id      = aws_vpc.main.id

  /*ingress {
    protocol    = "tcp"
    from_port   = 22
    to_port     = 22
    cidr_blocks = ["165.225.0.0/16"]
  }*/

  egress {
    protocol    = "tcp"
    from_port   = 443
    to_port     = 443
    cidr_blocks = ["0.0.0.0/0"]
  }

  /*egress {
    protocol    = "tcp"
    from_port   = 80
    to_port     = 80
    cidr_blocks = ["0.0.0.0/0"]
  }*/

  egress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["${var.vpc_cidr_block}"]
  }

  egress {
    from_port   = 27017
    to_port     = 27017
    protocol    = "tcp"
    cidr_blocks = ["${var.vpc_cidr_block}"]
  }
  egress {
    from_port   = 5432
    to_port     = 5432
    protocol    = "tcp"
    cidr_blocks = ["${var.vpc_cidr_block}"]
  }

  tags = local.common_tags
}
