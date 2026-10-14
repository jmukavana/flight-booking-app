# Terraform GCP — production IaC skeleton

This directory contains the initial Terraform skeleton for provisioning the Flight Booking App on Google Cloud Platform. 
It mirrors the architecture intended for AWS (EKS) by targeting Google Kubernetes Engine (GKE).

## Target shape

```
terraform-gcp/
  modules/
    network/        VPC, subnets, Cloud NAT
    gke/            GKE cluster + node pools
    registry/       Google Artifact Registry (for Docker images)
    data/           Cloud SQL (Postgres), Memorystore (Redis), Confluent/MSK (Kafka)
  envs/
    dev/            Development environment deployment
    prod/           Production environment deployment
```

## Usage

You will need a GCP project and appropriate IAM permissions.

```bash
cd envs/dev
terraform init
terraform plan
terraform apply
```
