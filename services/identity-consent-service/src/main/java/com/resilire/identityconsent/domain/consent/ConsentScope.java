package com.resilire.identityconsent.domain.consent;

/**
 * What a Traveller may consent to. Part of the ubiquitous language of the
 * Identity &amp; Consent context. AUTO_RECOVERY gates the Recovery context's
 * authority engine (architecture.md §9).
 */
public enum ConsentScope {
    AUTO_RECOVERY,
    MARKETING,
    DATA_PROCESSING,
    THIRD_PARTY_CLAIMS
}
