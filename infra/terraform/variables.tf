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

variable "database_port" {
  type        = number
  description = "Private PostgreSQL listener port."
  default     = 5432
}

variable "artifact_storage_path" {
  type        = string
  description = "Private artifact storage location mounted for the application."
  default     = "/var/lib/healthforge/artifacts"
}
