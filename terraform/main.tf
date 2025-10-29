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

data "aws_ecs_cluster" "cluster" {
  cluster_name = var.ecs_cluster_name
}

# Create the ALB security group
resource "aws_security_group" "alb_sg" {
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
}

# Look up the VPC ID automatically from one of your subnets
data "aws_subnet" "first" {
  id = var.alb_public_subnets[0]
}
data "aws_vpc" "selected" {
  id = data.aws_subnet.first.vpc_id
}

# ALB
resource "aws_lb" "this" {
  name               = "login-api-alb"
  internal           = false
  load_balancer_type = "application"
  security_groups    = [aws_security_group.alb_sg.id]
  subnets            = var.alb_public_subnets
}

# Target group for ECS tasks
resource "aws_lb_target_group" "login_api" {
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

# Listener
resource "aws_lb_listener" "http" {
  load_balancer_arn = aws_lb.this.arn
  port              = 80
  protocol          = "HTTP"

  default_action {
    type             = "forward"
    target_group_arn = aws_lb_target_group.login_api.arn
  }
}

# Log group
resource "aws_cloudwatch_log_group" "login_api" {
  name              = "/ecs/login-api-task"
  retention_in_days = 14
}

# IAM roles for ECS tasks
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
  name               = "login-api-exec-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}
resource "aws_iam_role_policy_attachment" "exec_attach" {
  role       = aws_iam_role.exec_role.name
  policy_arn = "arn:aws:iam::aws:policy/service-role/AmazonECSTaskExecutionRolePolicy"
}

resource "aws_iam_role" "task_role" {
  name               = "login-api-task-role"
  assume_role_policy = data.aws_iam_policy_document.ecs_assume.json
}

# Task definition
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
          "awslogs-group"         = aws_cloudwatch_log_group.login_api.name
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

# ECS Service
resource "aws_ecs_service" "login_api" {
  name            = "login-api-service"
  cluster         = data.aws_ecs_cluster.cluster.arn
  task_definition = aws_ecs_task_definition.login_api.arn
  desired_count   = var.service_desired_count
  launch_type     = "FARGATE"

  network_configuration {
    subnets         = var.service_subnets
    security_groups = [var.service_security_group_id]
    assign_public_ip = true
  }

  load_balancer {
    target_group_arn = aws_lb_target_group.login_api.arn
    container_name   = "login-api"
    container_port   = var.container_port
  }

  depends_on = [aws_lb_listener.http]
}
