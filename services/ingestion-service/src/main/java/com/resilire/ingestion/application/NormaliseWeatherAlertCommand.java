package com.resilire.ingestion.application;

import java.util.List;

public record NormaliseWeatherAlertCommand(
        String alertId,
        String severity,
        List<String> airports,
        String description,
        String validFrom,
        String validUntil,
        String source) {
}
