data "aws_ecr_repository" "repo" {
  name = "${var.PROJECT_NAME}-repo"
}

data "template_file" "container_definitions" {
  template = file("./container-definitions.json.tpl")

  vars = {
    REPOSITORY_URL                                 = "${replace("${data.aws_ecr_repository.repo.repository_url}", "https://", "")}"
    SPRING_DATA_MONGODB_URL                        = data.terraform_remote_state.core_tfstate.outputs.docdb_database_endpoint
    SPRING_DATA_MONGODB_USERNAME                   = data.terraform_remote_state.core_tfstate.outputs.docdb_database_username
    SPRING_DATA_MONGODB_PASSWORD                   = data.terraform_remote_state.core_tfstate.outputs.docdb_database_password
    SPRING_DATASOURCE_PASSWORD                     = data.terraform_remote_state.core_tfstate.outputs.database_password_ssm_parameter_arn
    SPRING_DATA_MONGODB_DATABASE                   = data.terraform_remote_state.core_tfstate.outputs.docdb_database_name
    APP_BUCKET_NAME                                = data.terraform_remote_state.core_tfstate.outputs.master_appdata_s3_bucket_id
    SERVER_PORT                                    = var.SERVER_PORT
    SERVER_SERVLET_CONTEXT_PATH                    = var.SERVER_SERVLET_CONTEXT_PATH
    SECURITY_OAUTH2_RESOURCESERVER_JWT_JWT_SET_URI = var.SECURITY_OAUTH2_RESOURCESERVER_JWT_JWT_SET_URI
    TAG_SUFFIX                                     = "${var.CI_PIPELINE_ID}_${lower(terraform.workspace)}"
    app_name                                       = var.app_name
    log_group_name                                 = "${var.app_name}-${data.terraform_remote_state.core_tfstate.outputs.prefix}"
    log_group_region                               = data.aws_region.current.name
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
  name           = "admin-service"
  pattern        = "ERROR"
  log_group_name = "${var.app_name}-${data.terraform_remote_state.core_tfstate.outputs.prefix}"

  metric_transformation {
    name      = "EventCount"
    namespace = "custommetrics"
    value     = "1"
  }
}


