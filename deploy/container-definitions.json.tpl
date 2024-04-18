[
    {
        "name": "${app_name}",
        "image": "${REPOSITORY_URL}:${app_name}_${TAG_SUFFIX}",
        "essential": true,
        "memoryReservation": 256,
        "environment": [           
            {"name": "SPRING_DATA_MONGODB_URI", "value": "mongodb://${SPRING_DATA_MONGODB_USERNAME}:${SPRING_DATA_MONGODB_PASSWORD}@${SPRING_DATA_MONGODB_URL}:27017/${SPRING_DATA_MONGODB_DATABASE}?ssl=true&replicaSet=rs0&readPreference=secondaryPreferred&retryWrites=false"},
            {"name": "SERVER_PORT", "value": "${SERVER_PORT}"},
            {"name": "SERVER_SERVLET_CONTEXT_PATH", "value": "${SERVER_SERVLET_CONTEXT_PATH}"},
            {"name": "SECURITY_OAUTH2_RESOURCESERVER_JWT_JWT_SET_URI", "value": "${SECURITY_OAUTH2_RESOURCESERVER_JWT_JWT_SET_URI}"},
            {"name":"S3_BUCKET","value":"${APP_BUCKET_NAME}"}
            
        ],

        "secrets": [
            {"name": "SPRING_DATASOURCE_PASSWORD", "valueFrom": "${SPRING_DATASOURCE_PASSWORD}"},
            {"name": "SPRING_DATA_MONGODB_PASSWORD", "valueFrom": "${SPRING_DATASOURCE_PASSWORD}"}
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
