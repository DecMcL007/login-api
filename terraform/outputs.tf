##########################################
# OUTPUTS
##########################################

output "alb_dns_name" {
  description = "DNS name of the Application Load Balancer"
  value       = aws_lb.this.dns_name
}

output "alb_security_group_id" {
  description = "ID of the ALB security group"
  value       = local.alb_sg_id
}

output "target_group_arn" {
  description = "ARN of the target group used by ECS service"
  value       = local.target_group_arn
}

output "log_group_name" {
  description = "Name of the CloudWatch log group used by ECS tasks"
  value       = local.log_group_name
}

output "ecs_service_name" {
  description = "Name of the ECS Service"
  value       = aws_ecs_service.login_api.name
}

output "ecs_task_definition_arn" {
  description = "Task Definition ARN for the login-api container"
  value       = aws_ecs_task_definition.login_api.arn
}

output "ecs_cluster_name" {
  description = "ECS cluster name used for deployment"
  value       = data.aws_ecs_cluster.cluster.cluster_name
}