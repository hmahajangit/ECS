//ecs details
desired_count          = "1"
health_check_path      = "/health"
SERVER_PORT            = 80
task_definition_cpu    = 512
task_definition_memory = 1024

//ecs capacity provider details
ecs_fargate_weight      = 34
ecs_fargate_spot_weight = 66
ecs_fargate_base        = 1



//app scaling 
min_capacity              = 1
max_capacity              = 1
cpu_target_value          = 75
cpu_scale_in_cooldown     = 300
cpu_scale_out_cooldown    = 120
memory_target_value       = 75
memory_scale_in_cooldown  = 300
memory_scale_out_cooldown = 120