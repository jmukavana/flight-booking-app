package com.resilire.identityconsent.infrastructure.web;

import com.resilire.identityconsent.application.ConsentApplicationService;
import com.resilire.identityconsent.domain.consent.ConsentEntry;
import com.resilire.identityconsent.domain.consent.ConsentScope;
import com.resilire.identityconsent.domain.consent.UserId;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Inbound adapter. The Traveller is never trusted from the request body — the
 * {@link UserId} is always the authenticated OIDC subject.
 */
@RestController
@RequestMapping("/consents")
public class ConsentController {

    private final ConsentApplicationService consent;

    public ConsentController(ConsentApplicationService consent) {
        this.consent = consent;
    }

    @PutMapping("/{scope}")
    public ResponseEntity<Void> grant(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable ConsentScope scope) {
        consent.grantConsent(currentUser(jwt), scope);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{scope}")
    public ResponseEntity<Void> revoke(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable ConsentScope scope) {
        consent.revokeConsent(currentUser(jwt), scope);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<String> effective(@AuthenticationPrincipal Jwt jwt) {
        return consent.effectiveConsents(currentUser(jwt)).stream()
                .map(Enum::name)
                .sorted()
                .toList();
    }

    @GetMapping("/history")
    public List<HistoryItem> history(@AuthenticationPrincipal Jwt jwt) {
        return consent.history(currentUser(jwt)).stream()
                .map(HistoryItem::of)
                .toList();
    }

    private UserId currentUser(Jwt jwt) {
        return UserId.of(jwt.getSubject());
    }

    public record HistoryItem(String scope, String decision, long version, Instant occurredAt) {
        static HistoryItem of(ConsentEntry e) {
            return new HistoryItem(e.scope().name(), e.decision().name(), e.version(), e.occurredAt());
        }
    }
}
