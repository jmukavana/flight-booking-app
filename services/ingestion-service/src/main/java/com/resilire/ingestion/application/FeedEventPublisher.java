package com.resilire.ingestion.application;

import com.resilire.ingestion.domain.feed.FlightStatusEvent;
import com.resilire.ingestion.domain.feed.WeatherAlertEvent;

public interface FeedEventPublisher {
    void publishFlightStatus(FlightStatusEvent event);
    void publishWeatherAlert(WeatherAlertEvent event);
}
