resource "aws_iam_role" "task_execution_role" {
  name = "${local.prefix}-task-exec-role"
  assume_role_policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Action" : "sts:AssumeRole",
        "Principal" : {
          "Service" : "ecs-tasks.amazonaws.com"
        },
        "Effect" : "Allow",
        "Sid" : ""
      }
    ]
  })
  tags = local.common_tags
}

resource "aws_iam_role_policy" "task_execution_policy" {
  name = "${local.prefix}-task-execution-policy"
  role = aws_iam_role.task_execution_role.id
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Effect" : "Allow",
        "Action" : [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
          "logs:CreateLogGroup",
          "logs:DeleteLogGroup",
          "logs:DescribeLogGroups",
          "logs:ListTagsLogGroup",
          "logs:TagLogGroup",
          "logs:DescribeLogStreams",
          "ssm:GetParameters"
        ],
        "Resource" : "*"
      }
    ]
  })
}

resource "aws_iam_role" "app_iam_role" {
  name = "${local.prefix}-app-task"
  assume_role_policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Action" : "sts:AssumeRole",
        "Principal" : {
          "Service" : "ecs-tasks.amazonaws.com"
        },
        "Effect" : "Allow",
        "Sid" : ""
      }
    ]
  })

  tags = local.common_tags
}

resource "aws_iam_policy" "ecstask_resources_access" {
  name        = "${local.prefix}-ecstask-resourcesAccessPolicy"
  path        = "/"
  description = "Allow access from apps to aws resources"

  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Effect" : "Allow",
        "Action" : [
          "s3:PutObject",
          "s3:GetObjectAcl",
          "s3:GetObject",
          "s3:ListBucket",
          "s3:DeleteObject",
          "s3:PutObjectAcl",
          "ssm:GetParameters",
          "ssm:GetParameter",
          "ssm:GetParametersByPath",
          "ecs:CreateCluster",
          "ecs:DeregisterContainerInstance",
          "ecs:DiscoverPollEndpoint",
          "ecs:Poll",
          "ecs:RegisterContainerInstance",
          "ecs:StartTelemetrySession",
          "ecs:Submit*",
          "ecs:UpdateContainerInstancesState",
          "ecs:RegisterTaskDefinition",
          "ecs:DeregisterTaskDefinition",
          "ecs:DescribeTaskDefinition",
          "ecs:ListTaskDefinitions",
          "ecs:DeleteCluster",
          "ecs:DescribeClusters",
          "ecs:ListClusters",
          "ecs:ListContainerInstances",
          "ecs:DescribeContainerInstances",
          "ecs:UpdateService",
          "ecs:CreateService",
          "ecs:DeleteService",
          "ecs:DescribeServices",
          "ecs:ListServices",
          "ecs:RunTask",
          "ecs:StopTask",
          "ecs:DescribeTasks",
          "ecs:ListTasks",
          "ecs:UpdateContainerInstancesState",
          "ecs:CreateTaskSet",
          "ecs:DeleteTaskSet",
          "ecs:DescribeTaskSets",
          "ecs:UpdateServicePrimaryTaskSet",
          "logs:*"
        ],
        "Resource" : "*"
      }
    ]
  })
}

resource "aws_iam_role_policy_attachment" "ecstask_resources_attachment" {
  role       = aws_iam_role.app_iam_role.name
  policy_arn = aws_iam_policy.ecstask_resources_access.arn
}

# ecs service role
resource "aws_iam_role" "ecs_service_role" {
  name = "${local.prefix}-ecs-service"
  assume_role_policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Action" : "sts:AssumeRole",
        "Principal" : {
          "Service" : "ecs.amazonaws.com"
        },
        "Effect" : "Allow",
        "Sid" : ""
      }
    ]
  })
  tags = local.common_tags
}

resource "aws_iam_role_policy" "ecs-servicepolicy" {
  name = "${local.prefix}-ecs-service-policy"
  role = aws_iam_role.ecs_service_role.id
  policy = jsonencode({
    "Version" : "2012-10-17",
    "Statement" : [
      {
        "Effect" : "Allow",
        "Action" : [
          "ecs:DescribeServices",
          "ecs:UpdateService",
          "cloudwatch:PutMetricAlarm",
          "cloudwatch:DescribeAlarms",
          "cloudwatch:DeleteAlarms"
        ],
        "Resource" : "*"
      }
    ]
  })
}

output "task_execution_role" {
  value = aws_iam_role.task_execution_role.arn
}
output "app_iam_role" {
  value = aws_iam_role.app_iam_role.arn
}
output "ecs_service_role" {
  value = aws_iam_role.ecs_service_role.arn
}

