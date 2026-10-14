package com.resilire.recoveryorchestrator.application;

import com.resilire.recoveryorchestrator.domain.RecoveryCaseStatus;
import com.resilire.recoveryorchestrator.domain.RecoveryOptionType;
import com.resilire.recoveryorchestrator.infrastructure.persistence.RecoveryCaseJpaRepository;
import com.resilire.recoveryorchestrator.infrastructure.persistence.RecoveryCaseRecord;
import com.resilire.recoveryorchestrator.infrastructure.persistence.RecoveryOptionJpaRepository;
import com.resilire.recoveryorchestrator.infrastructure.persistence.RecoveryOptionRecord;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RecoveryOrchestratorService {

    private final RecoveryCaseJpaRepository cases;
    private final RecoveryOptionJpaRepository options;
    private final RecoveryEventPublisher publisher;
    private final boolean autoEnabled;

    public RecoveryOrchestratorService(RecoveryCaseJpaRepository cases,
                                       RecoveryOptionJpaRepository options,
                                       RecoveryEventPublisher publisher,
                                       @Value("${resilire.recovery.auto-enabled:false}") boolean autoEnabled) {
        this.cases = cases;
        this.options = options;
        this.publisher = publisher;
        this.autoEnabled = autoEnabled;
    }

    public RecoveryCaseRecord openCase(String disruptionId, String flightId, String origin,
                                       String destination, String cause, String severity,
                                       int delayMinutes) {
        return openCase(disruptionId, flightId, origin, destination, cause, severity, delayMinutes, null);
    }

    public RecoveryCaseRecord openCase(String disruptionId, String flightId, String origin,
                                       String destination, String cause, String severity,
                                       int delayMinutes, Integer distanceKm) {
        return cases.findByDisruptionId(disruptionId).orElseGet(() -> {
            RecoveryCaseRecord c = new RecoveryCaseRecord();
            c.setId(UUID.randomUUID().toString());
            c.setDisruptionId(disruptionId);
            c.setFlightId(flightId);
            c.setOrigin(origin);
            c.setDestination(destination);
            c.setDisruptionCause(cause);
            c.setSeverity(severity);
            c.setDelayMinutes(delayMinutes);
            c.setStatus(RecoveryCaseStatus.OPEN);
            c.setAutoApproved(autoEnabled);
            c.setOpenedAt(Instant.now());
            cases.save(c);

            List<RecoveryOptionType> proposed = proposeOptions(cause, severity, delayMinutes, distanceKm);
            proposed.forEach(type -> {
                RecoveryOptionRecord opt = new RecoveryOptionRecord();
                opt.setId(UUID.randomUUID().toString());
                opt.setCaseId(c.getId());
                opt.setOptionType(type);
                opt.setDescription(type.description());
                opt.setCreatedAt(Instant.now());
                options.save(opt);
            });

            publisher.publishCaseOpened(c, proposed);
            return c;
        });
    }

    public RecoveryCaseRecord resolveCase(String id) {
        RecoveryCaseRecord c = cases.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Recovery case not found: " + id));
        c.setStatus(RecoveryCaseStatus.RESOLVED);
        c.setResolvedAt(Instant.now());
        return cases.save(c);
    }

    public RecoveryCaseRecord failCase(String id) {
        RecoveryCaseRecord c = cases.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Recovery case not found: " + id));
        c.setStatus(RecoveryCaseStatus.FAILED);
        c.setResolvedAt(Instant.now());
        return cases.save(c);
    }

    public Optional<RecoveryCaseRecord> findById(String id) {
        return cases.findById(id);
    }

    public List<RecoveryCaseRecord> findByStatus(RecoveryCaseStatus status) {
        return cases.findByStatus(status);
    }

    public List<RecoveryOptionRecord> findOptions(String caseId) {
        return options.findByCaseId(caseId);
    }

    private List<RecoveryOptionType> proposeOptions(String cause, String severity,
                                                     int delayMinutes, Integer distanceKm) {
        boolean cancelled = "FLIGHT_CANCELLED".equals(cause) || "CANCELLED".equals(severity);
        boolean shortHaul = distanceKm != null && distanceKm <= 500;

        if (cancelled) {
            if (shortHaul) {
                return List.of(RecoveryOptionType.FULL_REFUND,
                               RecoveryOptionType.REBOOKING,
                               RecoveryOptionType.GROUND_TRANSPORT);
            }
            return List.of(RecoveryOptionType.FULL_REFUND, RecoveryOptionType.REBOOKING);
        }
        if ("SEVERE".equals(severity)) {
            if (delayMinutes >= 240) {
                return List.of(RecoveryOptionType.REBOOKING,
                               RecoveryOptionType.HOTEL_ACCOMMODATION,
                               RecoveryOptionType.MEAL_VOUCHER);
            }
            return List.of(RecoveryOptionType.REBOOKING, RecoveryOptionType.MEAL_VOUCHER);
        }
        return List.of(RecoveryOptionType.MEAL_VOUCHER);
    }
}
