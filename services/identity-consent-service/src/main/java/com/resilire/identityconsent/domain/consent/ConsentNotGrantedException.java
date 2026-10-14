package com.resilire.identityconsent.domain.consent;

/** Raised when a Traveller tries to revoke a scope that is not currently effective. */
public class ConsentNotGrantedException extends RuntimeException {

    public ConsentNotGrantedException(UserId userId, ConsentScope scope) {
        super("Consent for scope %s is not currently granted for user %s"
                .formatted(scope, userId.value()));
    }
}
