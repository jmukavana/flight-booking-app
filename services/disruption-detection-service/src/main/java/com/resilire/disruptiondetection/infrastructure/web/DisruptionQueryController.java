package com.resilire.disruptiondetection.infrastructure.web;

import com.resilire.disruptiondetection.application.DisruptionDetectionService;
import com.resilire.disruptiondetection.domain.disruption.Disruption;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

/**
 * Read-only query API for active disruptions. Called by recovery-orchestrator
 * (Phase 2) and any internal tooling that needs the live disruption state.
 * Internal service — no gateway route in Phase 1.
 */
@RestController
@RequestMapping("/disruptions")
class DisruptionQueryController {

    private final DisruptionDetectionService service;

    DisruptionQueryController(DisruptionDetectionService service) {
        this.service = service;
    }

    @GetMapping("/active")
    Flux<DisruptionView> activeDisruptions() {
        return Flux.defer(() -> Flux.fromIterable(service.activeDisruptions())
                        .map(DisruptionView::from))
                .subscribeOn(Schedulers.boundedElastic());
    }

    record DisruptionView(
            String disruptionId,
            String flightId,
            String origin,
            String destination,
            String cause,
            String severity,
            int delayMinutes,
            Instant detectedAt) {

        static DisruptionView from(Disruption d) {
            return new DisruptionView(
                    d.id().value(),
                    d.flight().flightId(),
                    d.flight().origin(),
                    d.flight().destination(),
                    d.cause().name(),
                    d.severity().name(),
                    d.delayMinutes(),
                    d.detectedAt());
        }
    }
}
