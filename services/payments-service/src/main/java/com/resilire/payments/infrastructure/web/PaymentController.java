package com.resilire.payments.infrastructure.web;

import com.resilire.payments.application.CaptureCommand;
import com.resilire.payments.application.PaymentApplicationService;
import com.resilire.payments.application.RefundCommand;
import com.resilire.payments.domain.payment.LedgerEntry;
import com.resilire.payments.domain.payment.Money;
import com.resilire.payments.domain.payment.TravellerId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal adapter for tokenised capture/refund (SDD §5.1). Called service-to-service
 * (e.g. Booking) with the Traveller's relayed token — the payer is the JWT subject,
 * never the request body. Both endpoints require an {@code idempotencyKey} so a retry
 * cannot move money twice.
 */
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentApplicationService payments;

    public PaymentController(PaymentApplicationService payments) {
        this.payments = payments;
    }

    @PostMapping("/captures")
    public PaymentView capture(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CaptureRequest request) {
        LedgerEntry entry = payments.capture(new CaptureCommand(
                currentTraveller(jwt), new Money(request.amount(), request.currency()), request.idempotencyKey()));
        return PaymentView.of(entry);
    }

    @PostMapping("/refunds")
    public PaymentView refund(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody RefundRequest request) {
        LedgerEntry entry = payments.refund(new RefundCommand(
                currentTraveller(jwt), request.capturedRef(), request.idempotencyKey()));
        return PaymentView.of(entry);
    }

    private TravellerId currentTraveller(Jwt jwt) {
        return TravellerId.of(jwt.getSubject());
    }

    public record CaptureRequest(@NotNull BigDecimal amount, @NotBlank String currency,
                                 @NotBlank String idempotencyKey) {}

    public record RefundRequest(@NotBlank String capturedRef, @NotBlank String idempotencyKey) {}

    public record PaymentView(String paymentRef, String type, String amount, String currency,
                              String idempotencyKey, Instant recordedAt) {
        static PaymentView of(LedgerEntry e) {
            return new PaymentView(e.processorRef(), e.type().name(), e.amount().amount().toPlainString(),
                    e.amount().currency(), e.idempotencyKey(), e.recordedAt());
        }
    }
}
