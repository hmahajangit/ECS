data "aws_ecr_repository" "repo" {
  name = "${var.PROJECT_NAME}-repo"
}

data "template_file" "container_definitions" {
  template = file("./container-definitions.json.tpl")

  vars = {
    REPOSITORY_URL   = "${replace("${data.aws_ecr_repository.repo.repository_url}", "https://", "")}"
    SERVER_PORT      = var.SERVER_PORT
    app_name         = var.app_name
    IMAGE_VERSION    = "${var.app_name}_${var.CI_PIPELINE_ID}_${terraform.workspace}"
    log_group_name   = "${var.app_name}-${data.terraform_remote_state.core_tfstate.outputs.prefix}"
    log_group_region = data.aws_region.current.name
  }
}

resource "aws_ecs_task_definition" "task_definition" {
  family                   = "${data.terraform_remote_state.core_tfstate.outputs.prefix}-${var.app_name}"
  container_definitions    = data.template_file.container_definitions.rendered
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = var.task_definition_cpu
  memory                   = var.task_definition_memory
  execution_role_arn       = data.terraform_remote_state.core_tfstate.outputs.task_execution_role
  task_role_arn            = data.terraform_remote_state.core_tfstate.outputs.app_iam_role

  volume {
    name = "static"
  }

  tags = merge(
    data.terraform_remote_state.core_tfstate.outputs.common_tags,
    { "Name" = "${data.terraform_remote_state.core_tfstate.outputs.prefix}-${var.app_name}" },
  )

}

resource "aws_ecs_service" "ecs_service" {
  name            = "${data.terraform_remote_state.core_tfstate.outputs.prefix}-${var.app_name}"
  cluster         = data.terraform_remote_state.core_tfstate.outputs.ecs_cluster_id
  task_definition = aws_ecs_task_definition.task_definition.family
  desired_count   = var.desired_count
  //launch_type      = "FARGATE"
  platform_version = "1.4.0"
  #iam_role         = data.terraform_remote_state.core_tfstate.outputs.ecs_service_role

  capacity_provider_strategy {
    capacity_provider = "FARGATE"
    weight            = var.ecs_fargate_weight
    base              = var.ecs_fargate_base
  }
  capacity_provider_strategy {
    capacity_provider = "FARGATE_SPOT"
    weight            = var.ecs_fargate_spot_weight
  }

  network_configuration {
    subnets         = data.terraform_remote_state.core_tfstate.outputs.private_subnets
    security_groups = [data.terraform_remote_state.core_tfstate.outputs.ecs_securitygroup_id]
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.target_group.arn
    container_name   = var.app_name
    container_port   = var.SERVER_PORT
  }

  propagate_tags = "SERVICE"
  tags = merge(
    data.terraform_remote_state.core_tfstate.outputs.common_tags,
    { "Name" = "${data.terraform_remote_state.core_tfstate.outputs.prefix}-${var.app_name}" },
  )
}

resource "aws_cloudwatch_log_metric_filter" "metric_filter" {
  name           = "nextworkui"
  pattern        = "ERROR"
  log_group_name = "${var.app_name}-${data.terraform_remote_state.core_tfstate.outputs.prefix}"

  metric_transformation {
    name      = "EventCount"
    namespace = "custommetrics"
    value     = "1"
  }
}