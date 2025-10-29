variable "aws_region" { default = "eu-west-1" }

variable "image_uri" { type = string }

variable "ecs_cluster_name" { default = "golf-cluster" }

variable "container_port" { default = 8082 }

# Network
variable "alb_public_subnets" {
  type    = list(string)
  default = ["subnet-0439022c27130a7eb", "subnet-0920b789870a4cdd0"]
}

# The ECS service (login-api) can reuse the same for now — public Fargate deployment
variable "service_subnets" {
  type    = list(string)
  default = ["subnet-0439022c27130a7eb", "subnet-0920b789870a4cdd0"]
}

# Security group that ECS tasks will use (existing one)
variable "service_security_group_id" {
  type    = string
  default = "sg-094d40a41b39d90be"
}

variable "service_desired_count" { default = 1 }
