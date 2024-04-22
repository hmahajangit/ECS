#TODO: Move ALB to private and use API gateway private integration incase architecture design changes.
resource "aws_lb" "public" {
  name               = "${local.prefix}-main"
  internal           = false
  load_balancer_type = "application"
  subnets            = tolist(aws_subnet.public[*].id)

  security_groups = [aws_security_group.lb.id]

  tags = local.common_tags
}

resource "aws_lb_target_group" "default" {
  name        = "${local.prefix}-default"
  protocol    = "HTTP"
  vpc_id      = aws_vpc.main.id
  target_type = "ip"
  port        = 8000

  health_check {
    path = "/info"
  }
  tags = local.common_tags
}

/*
resource "aws_lb_listener" "default" {
  load_balancer_arn = aws_lb.public.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.default.arn
  }
  /*default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }
}
*/

resource "aws_lb_listener" "default_https" {
  load_balancer_arn = aws_lb.public.arn
  port              = 443
  protocol          = "HTTPS"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.default.arn
  }
  ssl_policy      = var.ssl_policy
  certificate_arn = aws_acm_certificate_validation.cert.certificate_arn
  /*default_action {
    type = "redirect"

    redirect {
      port        = "443"
      protocol    = "HTTPS"
      status_code = "HTTP_301"
    }
  }*/
  tags = local.common_tags
}

resource "aws_lb_listener_certificate" "prod_cert" {
  count           = terraform.workspace == "staging" || terraform.workspace == "uat" ? 0 : 1
  listener_arn    = aws_lb_listener.default_https.arn
  certificate_arn = var.prod_alb_certificate
}

resource "aws_route53_record" "default" {
  provider = "aws.staging"
  zone_id  = var.public_hosted_zone_id
  name     = var.route53_record_name
  type     = "A"

  alias {
    name                   = aws_lb.public.dns_name
    zone_id                = aws_lb.public.zone_id
    evaluate_target_health = true
  }
}


resource "aws_security_group" "lb" {
  description = "Allow access to Application Load Balancer"
  name        = "${local.prefix}-lb"
  vpc_id      = aws_vpc.main.id

  /*ingress {
    protocol  = "tcp"
    from_port = 80
    to_port   = 80
    cidr_blocks = [
      "165.225.64.0/18",
      "0.0.0.0/0"
    ]
  }*/

  ingress {
    protocol    = "tcp"
    from_port   = 443
    to_port     = 443
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    protocol    = "tcp"
    from_port   = 0
    to_port     = 65000
    cidr_blocks = ["0.0.0.0/0"]
  }

  tags = local.common_tags
}

resource "aws_wafv2_web_acl_association" "example" {
  resource_arn = aws_lb.public.arn
  web_acl_arn  = aws_wafv2_web_acl.waf.arn
}

########## OUTPUTS	############
output "lb_arn" {
  value = aws_lb.public.arn
}
/*output "listener_arn" {
  value = aws_lb_listener.default.arn
}*/
output "listener_arn_https" {
  value = aws_lb_listener.default_https.arn
}
output "lb_securitygroup_id" {
  value = aws_security_group.lb.id
}
output "lb_dns_name" {
  value = aws_lb.public.dns_name
}
output "lb_route53_fqdn" {
  value = aws_route53_record.default.fqdn
}