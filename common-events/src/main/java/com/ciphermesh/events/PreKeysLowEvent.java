package com.ciphermesh.events;

import java.time.Instant;

/**
 * Emitted when a device's one-time pre-key pool drops below the replenishment
 * threshold, signalling the client to generate and upload more keys.
 */
public record PreKeysLowEvent(
        String userId,
        int deviceId,
        long remaining,
        Instant occurredAt
) {
}
