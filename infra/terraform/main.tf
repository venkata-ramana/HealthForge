terraform {
  required_version = ">= 1.6.0"
}

locals {
  deployment_summary = {
    environment        = var.environment
    application_name   = "healthforge-platform-api"
    private_app_port   = var.application_port
    private_db_port    = var.database_port
    db_username        = var.db_username
    db_password_secret = var.db_password_secret_ref
    auth_mode          = var.auth_mode
    default_org_id     = var.default_organization_id
    model_enabled      = var.model_enabled
    organization_scope = var.organization_scope
  }
}
