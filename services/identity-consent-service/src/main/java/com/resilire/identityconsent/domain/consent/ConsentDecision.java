package com.resilire.identityconsent.domain.consent;

/** The two recordable decisions. Both are appended; nothing is ever mutated. */
public enum ConsentDecision {
    GRANT,
    REVOKE
}
