package com.resilire.disruptiondetection.application;

import com.resilire.disruptiondetection.domain.disruption.DisruptionDomainEvent;

public interface DisruptionEventPublisher {
    void publish(DisruptionDomainEvent event);
}
