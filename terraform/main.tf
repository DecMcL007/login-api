terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.50"
    }
  }
}

provider "aws" {
  region = var.aws_region
}

##########################################
# ECS Cluster
##########################################
data "aws_ecs_cluster" "cluster" {
  cluster_name = var.ecs_cluster_name
}

##########################################
# VPC
##########################################
data "aws_vpc" "selected" {
  id = "vpc-042a30daa4cc5bcdb"
}

##########################################
# ALB Security Group (reuse or create)
##########################################
data "aws_security_groups" "existing_alb_sg" {
  filter {
    name   = "group-name"
    values = ["login-api-alb-sg"]
  }

  filter {
    name   = "vpc-id"
    values = [data.aws_vpc.selected.id]
  }
}

resource "aws_security_group" "alb_sg" {
  count       = length(data.aws_security_groups.existing_alb_sg.ids) == 0 ? 1 : 0
  name        = "login-api-alb-sg"
  description = "Allow HTTP traffic to ALB"
  vpc_id      = data.aws_vpc.selected.id

  ingress {
    from_port   = 80
    to_port     = 80
    protocol    = "tcp"
    cidr_blocks = ["0.0.0.0/0"]
  }

  egress {
    from_port   = 0
    to_port     = 0
    protocol    = "-1"
    cidr_blocks = ["0.0.0.0/0"]
  }

  lifecycle {
    ignore_changes = [name]
  }
}

locals {
  alb_sg_id = (
    length(data.aws_security_groups.existing_alb_sg.ids) > 0 ?
    data.aws_security_groups.existing_alb_sg.ids[0] :
    aws_security_group.alb_sg[0].id
  )
}

##########################################
# Load Balancer (reuse or create)
##########################################

#Try to find an existing ALB by name
data "aws_lb" "existing" {
  name = "login-api-alb"
}

#Create a new one only if the lookup fails
resource "aws_lb" "this" {
  count              = can(data.aws_lb.existing.arn) ? 0 : 1
  name               = "login-api-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [local.alb_sg_id]
  subnets            = var.alb_public_subnets
}

#Local variable to use the right ARN in downstream references
locals {
  alb_arn = (
    can(data.aws_lb.existing.arn)
    ? data.aws_lb.existing.arn
    : aws_lb.this[0].arn
  )
}

##########################################
# Target Group (reuse or create)
##########################################
# Try to look up an existing target group by name.
data "aws_lb_target_group" "existing_tg" {
  name = "tg-login-api"
}

# Create it only if lookup fails.
resource "aws_lb_target_group" "login_api" {
  count = can(data.aws_lb_target_group.existing_tg.arn) ? 0 : 1

  name        = "tg-login-api"
  port        = var.container_port
  protocol    = "HTTP"
  target_type = "ip"
  vpc_id      = data.aws_vpc.selected.id

  health_check {
    path              = "/actuator/health"
    matcher           = "200"
    interval          = 30
    timeout           = 5
    healthy_threshold = 2
  }
}

locals {
  target_group_arn = (
    can(data.aws_lb_target_group.existing_tg.arn) ?
    data.aws_lb_target_group.existing_tg.arn :
    aws_lb_target_group.login_api[0].arn
  )
}

##########################################
# ALB Listener
##########################################
resource "aws_lb_listener" "http" {
  load_balancer_arn = local.alb_arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = local.target_group_arn
  }
}

##########################################
# CloudWatch Log Group (reuse or create)
##########################################
data "aws_cloudwatch_log_group" "existing_log" {
  name = "/ecs/login-api-task"
}

resource "aws_cloudwatch_log_group" "login_api" {
  count             = data.aws_cloudwatch_log_group.existing_log.name != "" ? 0 : 1
  name              = "/ecs/login-api-task"
  retention_in_days = 14

  lifecycle {
    ignore_changes = [name]
  }
}

locals {
  log_group_name = (
    data.aws_cloudwatch_log_group.existing_log.name != "" ?
    data.aws_cloudwatch_log_group.existing_log.name :
    aws_cloudwatch_log_group.login_api[0].name
  )
}

##########################################
# IAM roles
##########################################
data "aws_iam_policy_document" "ecs_assume" {
  statement {
    actions = ["sts:AssumeRole"]
    principals {
      type        = "Service"
      identifiers = ["ecs-tasks.amazonaws.com"]
    }
  }
}

resource "aws_iam_role" "exec_role" {
  name_prefix        = "login-api-exec-"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

resource "aws_iam_role" "task_role" {
  name_prefix        = "login-api-task-"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

resource "aws_iam_role_policy_attachment" "exec_attach" {
  role       = aws_iam_role.exec_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

##########################################
# ECS Task Definition
##########################################
resource "aws_ecs_task_definition" "login_api" {
  family                   = "login-api-task"
  network_mode             = "awsvpc"
  requires_compatibilities = ["FARGATE"]
  cpu                      = "512"
  memory                   = "1024"
  execution_role_arn       = aws_iam_role.exec_role.arn
  task_role_arn            = aws_iam_role.task_role.arn

  container_definitions = jsonencode([
    {
      name      = "login-api"
      image     = var.image_uri
      essential = true
      portMappings = [
        {
          containerPort = var.container_port
          protocol      = "tcp"
        }
      ]
      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = local.log_group_name
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }
      environment = [
        { name = "SPRING_PROFILES_ACTIVE", value = "ecs" },
        { name = "SERVER_PORT", value = tostring(var.container_port) }
      ]
    }
  ])
}

##########################################
# ECS Service
##########################################
resource "aws_ecs_service" "login_api" {
  name            = "login-api-service"
  cluster         = data.aws_ecs_cluster.cluster.arn
  task_definition = aws_ecs_task_definition.login_api.arn
  desired_count   = var.service_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets          = var.service_subnets
    security_groups  = [var.service_security_group_id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = local.target_group_arn
    container_name   = "login-api"
    container_port   = var.container_port
  }

  depends_on = [aws_lb_listener.http]
}