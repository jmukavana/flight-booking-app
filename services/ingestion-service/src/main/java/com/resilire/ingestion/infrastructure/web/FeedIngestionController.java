package com.resilire.ingestion.infrastructure.web;

import com.resilire.ingestion.application.FeedNormalisationService;
import com.resilire.ingestion.application.NormaliseFlightStatusCommand;
import com.resilire.ingestion.application.NormaliseWeatherAlertCommand;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

/**
 * Receives raw status and weather feed pushes from trusted internal adapters
 * (airline NDC status hooks, weather data brokers). No auth in Phase 1 — all
 * traffic originates inside the cluster. Normalises and publishes immediately
 * (hot path); accepts asynchronously and returns 202.
 */
@RestController
@RequestMapping("/feeds")
class FeedIngestionController {

    private final FeedNormalisationService service;

    FeedIngestionController(FeedNormalisationService service) {
        this.service = service;
    }

    @PostMapping("/flight-status")
    Mono<ResponseEntity<Void>> ingestFlightStatus(@RequestBody Mono<FlightStatusRequest> body) {
        return body
                .flatMap(req -> Mono.fromRunnable(
                        () -> service.normaliseFlightStatus(new NormaliseFlightStatusCommand(
                                req.flightNumber(), req.origin(), req.destination(),
                                req.status(), req.scheduledDeparture(), req.estimatedDeparture(),
                                req.source())))
                        .subscribeOn(Schedulers.boundedElastic()))
                .thenReturn(ResponseEntity.accepted().<Void>build());
    }

    @PostMapping("/weather-alerts")
    Mono<ResponseEntity<Void>> ingestWeatherAlert(@RequestBody Mono<WeatherAlertRequest> body) {
        return body
                .flatMap(req -> Mono.fromRunnable(
                        () -> service.normaliseWeatherAlert(new NormaliseWeatherAlertCommand(
                                req.alertId(), req.severity(), req.airports(),
                                req.description(), req.validFrom(), req.validUntil(),
                                req.source())))
                        .subscribeOn(Schedulers.boundedElastic()))
                .thenReturn(ResponseEntity.accepted().<Void>build());
    }

    record FlightStatusRequest(
            String flightNumber, String origin, String destination,
            String status, String scheduledDeparture, String estimatedDeparture,
            String source) {}

    record WeatherAlertRequest(
            String alertId, String severity, List<String> airports,
            String description, String validFrom, String validUntil,
            String source) {}
}
