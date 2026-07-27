variable "environment" {
  type        = string
  description = "Deployment environment name such as demo, staging, or private-prod."
  default     = "demo"
}

variable "organization_scope" {
  type        = string
  description = "Primary organization boundary for the private deployment."
  default     = "local.default"
}

variable "application_port" {
  type        = number
  description = "Private application listener port."
  default     = 8080
}

variable "db_username" {
  type        = string
  description = "Database username injected into the private deployment runtime."
  default     = "healthforge"
}

variable "db_password_secret_ref" {
  type        = string
  description = "Reference to the database password secret in a secret manager or deployment platform. Do not place raw secret values in Terraform variables."
  default     = "set-in-secret-manager"
}

variable "database_port" {
  type        = number
  description = "Private PostgreSQL listener port."
  default     = 5432
}

variable "auth_mode" {
  type        = string
  description = "Runtime authentication mode. Keep local_header only for local/private demos until enterprise identity integration is installed."
  default     = "local_header"
}

variable "default_organization_id" {
  type        = string
  description = "Fallback organization boundary used only when the selected auth mode requires it."
  default     = "local.default"
}

variable "artifact_storage_path" {
  type        = string
  description = "Private artifact storage location mounted for the application."
  default     = "/var/lib/healthforge/artifacts"
}

variable "model_enabled" {
  type        = bool
  description = "Whether external model features are enabled in the target environment. Keep false unless a separate security and policy review has approved it."
  default     = false
}
