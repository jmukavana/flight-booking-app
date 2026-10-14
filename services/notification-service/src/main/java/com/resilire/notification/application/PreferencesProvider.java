package com.resilire.notification.application;

import com.resilire.notification.domain.notification.NotificationPreferences;
import com.resilire.notification.domain.notification.TravellerId;

/**
 * Outbound port for a Traveller's notification preferences. Phase 1 returns a
 * default profile; the Phase 2 adapter reads per-Traveller prefs/consent (FR-8).
 */
public interface PreferencesProvider {

    NotificationPreferences forTraveller(TravellerId traveller);
}
