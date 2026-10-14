package com.resilire.itinerary.application;

import com.resilire.itinerary.domain.itinerary.Itinerary;
import com.resilire.itinerary.domain.itinerary.ItineraryRepository;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Write side of the CQRS itinerary read model: folds order events into the Redis
 * projection. Idempotent by the {@code occurredAt} high-water mark, so the
 * outbox's at-least-once, cross-topic delivery cannot double-apply or let a stale
 * redelivery overwrite newer state (architecture.md §7).
 */
@Service
public class ItineraryProjectionService {

    private static final Logger log = LoggerFactory.getLogger(ItineraryProjectionService.class);

    private final ItineraryRepository itineraries;

    public ItineraryProjectionService(ItineraryRepository itineraries) {
        this.itineraries = itineraries;
    }

    public void onOrderPlaced(OrderPlacedProjection command) {
        Optional<Itinerary> existing = itineraries.findById(command.itineraryId());
        if (existing.isPresent() && existing.get().supersededBy(command.occurredAt())) {
            log.debug("Skipping stale OrderCreated for itinerary {}", command.itineraryId().value());
            return;
        }
        itineraries.save(Itinerary.placed(
                command.itineraryId(),
                command.traveller(),
                List.of(command.leg()),
                command.totalAmount(),
                command.occurredAt(),
                command.occurredAt()));
    }

    public void onOrderCancelled(OrderCancelledProjection command) {
        Optional<Itinerary> existing = itineraries.findById(command.itineraryId());
        if (existing.isEmpty()) {
            // Single ordered producer means created precedes cancelled in practice;
            // a cancel for an unknown itinerary is logged rather than tombstoned.
            log.warn("OrderCancelled for unknown itinerary {}, ignoring", command.itineraryId().value());
            return;
        }
        Itinerary itinerary = existing.get();
        if (itinerary.supersededBy(command.occurredAt())) {
            log.debug("Skipping stale OrderCancelled for itinerary {}", command.itineraryId().value());
            return;
        }
        itinerary.cancel(command.occurredAt());
        itineraries.save(itinerary);
    }

    public void onScoreComputed(ScoreComputedProjection command) {
        Optional<Itinerary> existing = itineraries.findById(command.itineraryId());
        if (existing.isEmpty()) {
            // The score is computed from the same OrderCreated this service also
            // projects; projection (a Redis upsert) normally lands first. A score
            // for an unknown itinerary is logged rather than buffered (Phase 1).
            log.warn("ScoreComputed for unknown itinerary {}, ignoring", command.itineraryId().value());
            return;
        }
        Itinerary itinerary = existing.get();
        if (itinerary.scoreSupersededBy(command.score().scoredAt())) {
            log.debug("Skipping stale ScoreComputed for itinerary {}", command.itineraryId().value());
            return;
        }
        itinerary.attachScore(command.score());
        itineraries.save(itinerary);
    }
}
