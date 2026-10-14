variable "project_id" {
  type = string
}

variable "region" {
  type    = string
  default = "europe-west1"
}

variable "network_id" {
  type = string
}

variable "subnetwork_id" {
  type = string
}

variable "node_count" {
  type    = number
  default = 2
}
