terraform {
  required_version = ">= 1.6.0"
}

locals {
  deployment_summary = {
    environment        = var.environment
    application_name   = "healthforge-platform-api"
    private_app_port   = var.application_port
    private_db_port    = var.database_port
    organization_scope = var.organization_scope
  }
}
