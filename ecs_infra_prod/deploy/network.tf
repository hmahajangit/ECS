resource "aws_vpc" "main" {
  cidr_block           = var.vpc_cidr_block
  enable_dns_support   = true
  enable_dns_hostnames = true

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-vpc" },
  )
}

resource "aws_default_security_group" "default" {
  vpc_id = aws_vpc.main.id
  ingress {
    protocol    = "tcp"
    from_port   = 443
    to_port     = 443
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    protocol    = "-1"
    from_port   = 0
    to_port     = 0
    cidr_blocks = ["0.0.0.0/0"]
  }
}

resource "aws_internet_gateway" "main" {
  vpc_id = aws_vpc.main.id

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-main" },
  )
}

#####################################################
# Public Subnets - Inbound/Outbound Internet Access #
#####################################################
resource "aws_subnet" "public" {
  count                   = length(var.public_subnet_ipv4_cidrs)
  cidr_block              = var.public_subnet_ipv4_cidrs[count.index]
  map_public_ip_on_launch = true
  vpc_id                  = aws_vpc.main.id
  availability_zone       = data.aws_availability_zones.available.names[count.index]

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-public-${count.index}" },
  )
}

resource "aws_route_table" "public" {
  vpc_id = aws_vpc.main.id
  route {
    cidr_block = "0.0.0.0/0"
    gateway_id = aws_internet_gateway.main.id
  }

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-public" },
  )
}

resource "aws_route_table_association" "public" {
  count          = length(var.public_subnet_ipv4_cidrs)
  subnet_id      = aws_subnet.public[count.index].id
  route_table_id = aws_route_table.public.id
}

###################################################
# NATGW for private resources to reache internet  #
###################################################
resource "aws_eip" "natgwip" {
  domain = "vpc"
  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-natgwip" },
  )
}
resource "aws_nat_gateway" "natgw" {
  allocation_id = aws_eip.natgwip.id
  subnet_id     = aws_subnet.public[0].id

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-natgw" }
  )
}

###################################################
# Private Subnets - Outbound internet access only #
###################################################
resource "aws_subnet" "private" {
  count             = length(var.private_subnet_ipv4_cidrs)
  cidr_block        = var.private_subnet_ipv4_cidrs[count.index]
  vpc_id            = aws_vpc.main.id
  availability_zone = data.aws_availability_zones.available.names[count.index]

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-private-${count.index}" },
  )
}

resource "aws_route_table" "private" {
  vpc_id = aws_vpc.main.id

  tags = merge(
    local.common_tags,
    { "Name" = "${local.prefix}-private" },
  )
}

resource "aws_route_table_association" "private" {
  count          = length(var.private_subnet_ipv4_cidrs)
  subnet_id      = aws_subnet.private[count.index].id
  route_table_id = aws_route_table.private.id
}

resource "aws_route" "private_internet_out" {
  route_table_id         = aws_route_table.private.id
  nat_gateway_id         = aws_nat_gateway.natgw.id
  destination_cidr_block = "0.0.0.0/0"
}

########## OUTPUTS	############
output "vpc_id" {
  value = aws_vpc.main.id
}
output "vpc_cidr_block" {
  value = aws_vpc.main.cidr_block
}

output "public_subnets" {
  value = aws_subnet.public[*].id
}
output "private_subnets" {
  value = aws_subnet.private[*].id
}
output "public_subnets_cidr" {
  value = aws_subnet.public[*].cidr_block
}
output "private_subnets_cidr" {
  value = aws_subnet.private[*].cidr_block
}

output "natgw_id" {
  value = aws_nat_gateway.natgw.id
}
output "natgw_ip" {
  value = aws_nat_gateway.natgw.public_ip
}
