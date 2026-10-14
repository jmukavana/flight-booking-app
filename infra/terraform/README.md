# Terraform — production IaC (deliberately a skeleton)

**Status: scaffold only, not provisioned.** This is intentional, not unfinished work.

Per [`../../docs/roadmap.md`](../../docs/roadmap.md) §3 and the over-build discipline applied throughout this project, the production cloud estate is **not built until Phase 1 platform-foundation work starts with a real AWS account and Phase-0 gates passed**. Generating a full multi-region EKS estate now would be speculative: untestable (no account), and contradicting the "single region first" Phase-1 posture (architecture.md §17, ADR-15).

## Target shape (when built — architecture.md §13)

```
terraform/
  modules/
    network/        VPC, subnets, NAT — per region
    eks/            EKS cluster + node groups (multi-AZ)
    data/           RDS Postgres, ElastiCache Redis, MSK Kafka — region-pinned
    observability/  OTel collector, dashboards
    security/       IAM (least-priv), KMS, secrets manager
  envs/
    dev/  stage/  prod-eu/  prod-na/   # prod split = data-residency (ADR-12)
```

- **Phase 1:** `dev` + one prod region only (single-region posture).
- **Phase 2:** add the second prod region (EU/NA) for residency — **not before**, residency cannot be retrofitted after PII accumulates single-region (roadmap §7, rule 4).
- Kafka is **regional** (no cross-region PII replication, ADR-12); rulesets/registry are global read-only.

## Why no service-discovery infra

Discovery is Kubernetes-native (CoreDNS + `Service` objects); cross-service integration is Kafka events (ADR-1). No Eureka/Consul module by design — see the service-discovery rationale in the conversation/architecture.md §13.

## Until then

Local dev uses `../docker-compose.yml`, which mirrors the prod topology (Postgres, Redis, Kafka, OIDC, schema registry) at single-node scale.
