package com.ciphermesh.events;

import java.time.Instant;

/**
 * Emitted when a user registers a device and uploads its public pre-key bundle.
 * Carries only non-sensitive identifiers — never any key material.
 */
public record UserRegisteredEvent(
        String userId,
        int deviceId,
        int registrationId,
        Instant occurredAt
) {
}
