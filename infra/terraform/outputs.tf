output "deployment_summary" {
  value = local.deployment_summary
}

output "private_application_url" {
  value = "http://private-healthforge:${var.application_port}"
}
