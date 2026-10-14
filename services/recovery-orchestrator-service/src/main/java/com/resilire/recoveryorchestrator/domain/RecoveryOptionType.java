package com.resilire.recoveryorchestrator.domain;

public enum RecoveryOptionType {
    REBOOKING("Alternative flight rebooking offered"),
    HOTEL_ACCOMMODATION("Hotel accommodation for overnight disruption"),
    MEAL_VOUCHER("Meal voucher for delay exceeding 2 hours"),
    GROUND_TRANSPORT("Ground transport to final destination"),
    FULL_REFUND("Full refund for cancelled or diverted flight");

    private final String description;

    RecoveryOptionType(String description) {
        this.description = description;
    }

    public String description() {
        return description;
    }
}
