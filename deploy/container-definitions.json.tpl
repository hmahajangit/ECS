[
    {
        "name": "${app_name}",
        "image": "${REPOSITORY_URL}:${IMAGE_VERSION}",
        "essential": true,
        "memoryReservation": 256,
        "environment": [           
            {"name": "SERVER_PORT", "value": "${SERVER_PORT}"}
        ],
        "logConfiguration": {
            "logDriver": "awslogs",
            "options": {
                "awslogs-group": "${log_group_name}",
                "awslogs-create-group": "true",
                "awslogs-region": "${log_group_region}",
                "awslogs-stream-prefix": "${app_name}"
            }
        },
        "portMappings": [
            {
                "containerPort": ${SERVER_PORT}
            }
        ]
    }
]