resource "aws_lb_target_group" "target_group" {
  name        = "${data.terraform_remote_state.core_tfstate.outputs.prefix}-${substr(var.app_name, 0, 10)}-${substr(uuid(), 2, 1)}"
  protocol    = "HTTPS"
  vpc_id      = data.terraform_remote_state.core_tfstate.outputs.vpc_id
  target_type = "ip"
  port        = var.SERVER_PORT

  health_check {
    healthy_threshold   = "2"
    unhealthy_threshold = "3"
    interval            = "30"
    matcher             = "200,202"
    path                = var.health_check_path
    port                = "traffic-port"
    protocol            = "HTTPS"
    timeout             = "16"
  }
  lifecycle {
    create_before_destroy = true
    ignore_changes        = ["name"]
  }
  tags = merge(
    data.terraform_remote_state.core_tfstate.outputs.common_tags,
    { "Name" = "${data.terraform_remote_state.core_tfstate.outputs.prefix}-${substr(var.app_name, 0, 10)}" },
  )

}
resource "aws_alb_listener_rule" "route_path" {
  listener_arn = data.terraform_remote_state.core_tfstate.outputs.listener_arn_https
  priority     = 100
  action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.target_group.arn
  }
  condition {
    path_pattern {
      values = ["/*"]
    }
  }
  tags = merge(
    data.terraform_remote_state.core_tfstate.outputs.common_tags,
    { "Name" = "${data.terraform_remote_state.core_tfstate.outputs.prefix}-${substr(var.app_name, 0, 10)}" },
  )
}
