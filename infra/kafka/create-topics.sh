#!/usr/bin/env bash
# Creates all domain-event topics (architecture.md §7). One topic per event type,
# named to match the constants in each service's messaging layer.
# Partition key = flightId / itineraryId / claimId (producer-side); 6 partitions
# gives per-entity ordering with parallelism locally (prod = 3 replicas).
# Audit-critical streams get a .DLT so events are never silently dropped.
set -euo pipefail

TOPICS=(
  # ── ingestion-service ──────────────────────────────────────────────────────
  flight.status.updated          # → disruption-detection-service
  weather.alert.published        # → ingestion risk assessor (internal enrichment)

  # ── disruption-detection-service ───────────────────────────────────────────
  disruption.detected            # → recovery-orchestrator, notification
  disruption.resolved            # → notification, itinerary

  # ── booking-service ────────────────────────────────────────────────────────
  order.created                  # → resilience-score, itinerary, notification
  order.cancelled                # → itinerary, notification

  # ── resilience-score-service ───────────────────────────────────────────────
  score.computed                 # → itinerary

  # ── identity-consent-service ───────────────────────────────────────────────
  consent.changed                # → recovery-orchestrator (authority engine)

  # ── rights-claims-service ──────────────────────────────────────────────────
  claim.submitted                # → notification
  claim.approved                 # → payments, notification
  claim.paid                     # → notification

  # ── payments-service ───────────────────────────────────────────────────────
  payment.captured               # → notification, rights-claims
  payment.refunded               # → notification

  # ── recovery-orchestrator-service ──────────────────────────────────────────
  recovery.case.opened           # → notification, booking

  # ── notification-service ───────────────────────────────────────────────────
  notification.delivered         # → audit log (append-only)

  # ── Dead-letter topics for audit-bearing streams (never silently dropped) ──
  disruption.detected.DLT
  claim.submitted.DLT
  payment.captured.DLT
  recovery.case.opened.DLT
  consent.changed.DLT
)

K=/opt/kafka/bin/kafka-topics.sh
for t in "${TOPICS[@]}"; do
  # strip inline comments (words after '#')
  topic="${t%%#*}"
  topic="${topic// /}"
  [[ -z "$topic" ]] && continue
  "$K" --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
       --topic "$topic" --partitions 6 --replication-factor 1
done

echo "topics ready:"
"$K" --bootstrap-server "$BOOTSTRAP" --list
