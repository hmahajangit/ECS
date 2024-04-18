//db_username                 = "test"
//db_password                 = "test123$"
//ecs details
desired_count                                  = 1
health_check_path                              = "/admin-service/actuator/health"
SECURITY_OAUTH2_RESOURCESERVER_JWT_JWT_SET_URI = "https://myid-qa.siemens.com/ext/oauth/jwks"
SERVER_PORT                                    = 8080
SERVER_SERVLET_CONTEXT_PATH                    = "/admin-service"
task_definition_cpu                            = 512
task_definition_memory                         = 1024
alb_priority                                   = 11

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
