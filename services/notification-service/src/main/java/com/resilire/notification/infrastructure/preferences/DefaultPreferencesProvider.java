package com.resilire.notification.infrastructure.preferences;

import com.resilire.notification.application.PreferencesProvider;
import com.resilire.notification.domain.notification.NotificationPreferences;
import com.resilire.notification.domain.notification.TravellerId;
import org.springframework.stereotype.Component;

/**
 * Phase 1 preferences: the same default profile for everyone. Per-Traveller
 * preferences (channels, quiet hours) are FR-8, delivered in Phase 2 — a real
 * adapter reading the prefs/consent store drops in behind this port.
 */
@Component
class DefaultPreferencesProvider implements PreferencesProvider {

    @Override
    public NotificationPreferences forTraveller(TravellerId traveller) {
        return NotificationPreferences.defaults();
    }
}
