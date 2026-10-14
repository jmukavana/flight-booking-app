package com.resilire.booking.infrastructure.web;

import com.resilire.booking.application.BookingApplicationService;
import com.resilire.booking.application.PlaceOrderCommand;
import com.resilire.booking.domain.order.FlightOffer;
import com.resilire.booking.domain.order.Order;
import com.resilire.booking.domain.order.OrderId;
import com.resilire.booking.domain.order.SearchCriteria;
import com.resilire.booking.domain.order.TravellerId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Inbound adapter for the Booking context. The Traveller is always the
 * authenticated OIDC subject, never the request body. Order placement requires an
 * {@code Idempotency-Key} header so a retried submission cannot double-book (SDD §5.3).
 */
@RestController
public class BookingController {

    private final BookingApplicationService booking;

    public BookingController(BookingApplicationService booking) {
        this.booking = booking;
    }

    @GetMapping("/offers")
    public List<OfferView> search(@RequestParam String origin,
                                  @RequestParam String destination,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return booking.search(new SearchCriteria(origin, destination, date)).stream()
                .map(OfferView::of)
                .toList();
    }

    @PostMapping("/orders")
    public ResponseEntity<OrderView> place(@AuthenticationPrincipal Jwt jwt,
                                           @RequestHeader("Idempotency-Key") String idempotencyKey,
                                           @Valid @RequestBody PlaceOrderRequest request,
                                           UriComponentsBuilder uri) {
        Order order = booking.placeOrder(
                new PlaceOrderCommand(currentTraveller(jwt), request.offerId(), idempotencyKey));
        URI location = uri.path("/orders/{id}").buildAndExpand(order.id().value()).toUri();
        return ResponseEntity.created(location).body(OrderView.of(order));
    }

    @GetMapping("/orders")
    public List<OrderView> list(@AuthenticationPrincipal Jwt jwt) {
        return booking.listOrders(currentTraveller(jwt)).stream()
                .map(OrderView::of)
                .toList();
    }

    @GetMapping("/orders/{id}")
    public OrderView get(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return OrderView.of(booking.getOrder(currentTraveller(jwt), OrderId.of(id)));
    }

    @DeleteMapping("/orders/{id}")
    public OrderView cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable String id) {
        return OrderView.of(booking.cancelOrder(currentTraveller(jwt), OrderId.of(id)));
    }

    private TravellerId currentTraveller(Jwt jwt) {
        return TravellerId.of(jwt.getSubject());
    }

    public record PlaceOrderRequest(@NotBlank String offerId) {}

    public record OfferView(String offerId, String provider, String origin, String destination,
                            Instant departure, Instant arrival, String amount, String currency) {
        static OfferView of(FlightOffer o) {
            return new OfferView(o.offerId(), o.provider(), o.route().origin(), o.route().destination(),
                    o.route().departure(), o.route().arrival(),
                    o.price().amount().toPlainString(), o.price().currency());
        }
    }

    public record OrderView(String orderId, String itineraryId, String status, String provider,
                            String pnr, String origin, String destination, Instant departure,
                            Instant arrival, String amount, String currency, Instant placedAt) {
        static OrderView of(Order o) {
            return new OrderView(o.id().value().toString(), o.itineraryId().value().toString(),
                    o.status().name(), o.provider(), o.pnr().value(),
                    o.route().origin(), o.route().destination(),
                    o.route().departure(), o.route().arrival(),
                    o.amount().amount().toPlainString(), o.amount().currency(), o.placedAt());
        }
    }
}
