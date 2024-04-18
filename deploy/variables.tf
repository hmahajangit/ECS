variable "remote_state_bucket" {}
variable "remote_state_core_path" {}
variable "workspace_key_prefix" {}
variable "aws_region" {}
variable "PROJECT_NAME" {}
variable "CI_PIPELINE_ID" {}

//ecs details
variable "app_name" {}
variable "desired_count" {
  default = 1
}
variable "alb_priority" {
  default = "1"
}

variable "task_definition_cpu" {
  default = "512"
}
variable "task_definition_memory" {
  default = "1024"
}

variable "db_password" {}
variable "SERVER_PORT" {}
variable "SERVER_SERVLET_CONTEXT_PATH" {}
variable "SECURITY_OAUTH2_RESOURCESERVER_JWT_JWT_SET_URI" {}


variable "health_check_path" {
  default = "/actuator/health"
}

//ecs capacity provider details
variable "ecs_fargate_weight" {
  default = 34
}
variable "ecs_fargate_spot_weight" {
  default = 66
}
variable "ecs_fargate_base" {
  default = 1
}

//app scaling details
variable "min_capacity" { default = 1 }
variable "max_capacity" { default = 6 }
variable "scaling_policy_type" { default = "TargetTrackingScaling" }
variable "cpu_target_value" { default = 70 }
variable "cpu_disable_scale_in" { default = false }
variable "cpu_scale_in_cooldown" { default = 300 }
variable "cpu_scale_out_cooldown" { default = 300 }

variable "memory_target_value" { default = 70 }
variable "memory_disable_scale_in" { default = false }
variable "memory_scale_in_cooldown" { default = 300 }
variable "memory_scale_out_cooldown" { default = 300 }
