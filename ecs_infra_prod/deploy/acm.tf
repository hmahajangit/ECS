resource "aws_acm_certificate" "cert" {
  domain_name       = "${var.route53_record_name}.${var.domain_name}"
  validation_method = "DNS"

  tags = local.common_tags

  lifecycle {
    create_before_destroy = true
  }
}

resource "aws_route53_record" "cert_validation" {

  provider = "aws.staging"

  for_each = {
    for dvo in aws_acm_certificate.cert.domain_validation_options : dvo.domain_name => {
      name   = dvo.resource_record_name
      record = dvo.resource_record_value
      type   = dvo.resource_record_type
    }
  }

  name            = each.value.name
  type            = each.value.type
  zone_id         = var.public_hosted_zone_id
  records         = [each.value.record]
  allow_overwrite = true
  ttl             = 60
}

resource "aws_acm_certificate_validation" "cert" {
  certificate_arn = aws_acm_certificate.cert.arn
  #validation_record_fqdns = [for record in aws_route53_record.cert_validation : record.fqdn]
}

