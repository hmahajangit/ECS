# resource "tls_private_key" "self_signed_cert_key" {
#   algorithm = "RSA"
# }


# resource "tls_self_signed_cert" "self_signed_cert" {
#   allowed_uses = ["key_encipherment", "digital_signature"]

#   private_key_pem = tls_private_key.self_signed_cert_key.private_key_pem

#   subject {
#     common_name  = "internal.staging.nextworktool.siemens.cloud" # Replace with your internal domain name
#     organization = "Siemens-advanta"
#     country      = "India"
#   }

#   validity_period_hours = 8760 # 1 year
# }

# resource "aws_acm_certificate" "private_cert" {
#   private_key      = tls_private_key.self_signed_cert_key.private_key_pem
#   certificate_body = tls_self_signed_cert.self_signed_cert.cert_pem
#   lifecycle {
#     create_before_destroy = true
#   }
# }

# # resource "aws_lb_listener" "https_listener" {
# #   load_balancer_arn = aws_lb.public.arn
# #   port              = 8443
# #   protocol          = "HTTPS"
# #   ssl_policy        = var.ssl_policy

# #   default_action {
# #     type             = "forward"
# #     target_group_arn = aws_lb_target_group.default.arn
# #   }

# #   certificate_arn   = aws_acm_certificate.private_cert.arn
# # }
