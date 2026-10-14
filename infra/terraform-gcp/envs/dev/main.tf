terraform {
  required_providers {
    google = {
      source  = "hashicorp/google"
      version = "~> 5.0"
    }
  }
  # backend "gcs" {
  #   bucket  = "resilire-terraform-state"
  #   prefix  = "terraform/state/dev"
  # }
}

provider "google" {
  project = var.project_id
  region  = var.region
}

module "registry" {
  source     = "../../modules/registry"
  project_id = var.project_id
  region     = var.region
}

# The network and GKE modules would be wired here, assuming network is also created.
# module "network" { ... }
# module "gke" { ... }
