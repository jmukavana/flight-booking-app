package com.resilire.rightsclaims.domain;

/** Regulatory cause classification — distinct from the operational cause in disruption-detection-service. */
public enum DisruptionCause {
    /** Carrier-controllable: technical fault, staffing, late aircraft — compensation applies under EU261 etc. */
    CONTROLLABLE,
    /** Extraordinary circumstances: severe weather, ATC strike, security — no compensation obligation. */
    UNCONTROLLABLE
}
