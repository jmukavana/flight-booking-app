package com.resilire.booking.infrastructure.payment;

import com.resilire.booking.application.PaymentGateway;
import com.resilire.booking.domain.order.Money;
import com.resilire.booking.domain.order.PaymentDeclinedException;
import com.resilire.booking.domain.order.TravellerId;
import java.math.BigDecimal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Anti-corruption adapter over the Payments Service REST API (SDD §5.1).
 * Relays the traveller's JWT so Payments can enforce ownership — no PAN ever
 * reaches Booking (NFR-SEC-1). Any transport or 4xx/5xx failure collapses to
 * {@link PaymentDeclinedException} so the booking saga can surface a clean error.
 */
@Component
class HttpPaymentGateway implements PaymentGateway {

    private final RestClient payments;

    HttpPaymentGateway(RestClient paymentsRestClient) {
        this.payments = paymentsRestClient;
    }

    @Override
    public String capture(String idempotencyKey, Money amount, TravellerId traveller) {
        try {
            PaymentResponse response = payments.post()
                    .uri("/payments/captures")
                    .header("Authorization", "Bearer " + bearerToken())
                    .body(new CaptureRequest(amount.amount(), amount.currency(), idempotencyKey))
                    .retrieve()
                    .body(PaymentResponse.class);
            if (response == null) {
                throw new PaymentDeclinedException("empty response from payments-service");
            }
            return response.paymentRef();
        } catch (RestClientException e) {
            throw new PaymentDeclinedException(e.getMessage());
        }
    }

    @Override
    public void refund(String idempotencyKey, String paymentRef) {
        try {
            payments.post()
                    .uri("/payments/refunds")
                    .header("Authorization", "Bearer " + bearerToken())
                    .body(new RefundRequest(paymentRef, idempotencyKey))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            throw new PaymentDeclinedException(e.getMessage());
        }
    }

    private String bearerToken() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwt) {
            return jwt.getToken().getTokenValue();
        }
        throw new PaymentDeclinedException("no authenticated principal to relay to payments-service");
    }

    private record CaptureRequest(BigDecimal amount, String currency, String idempotencyKey) {}
    private record RefundRequest(String capturedRef, String idempotencyKey) {}
    private record PaymentResponse(String paymentRef) {}
}
