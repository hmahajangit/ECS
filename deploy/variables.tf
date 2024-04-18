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
variable "task_definition_cpu" {
  default = "512"
}
variable "task_definition_memory" {
  default = "1024"
}
variable "SERVER_PORT" {}

variable "health_check_path" {
  default = "/health"
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

//app scaling min_capacity
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