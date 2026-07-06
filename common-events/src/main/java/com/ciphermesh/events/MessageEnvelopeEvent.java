package com.ciphermesh.events;

import java.time.Instant;

/**
 * An end-to-end encrypted message in transit. The {@code ciphertext} is the
 * Double Ratchet output produced on the sender's device; the server treats it
 * as an opaque, base64-encoded blob and can never decrypt it.
 */
public record MessageEnvelopeEvent(
        String messageId,
        String senderId,
        String recipientId,
        int recipientDeviceId,
        String ciphertext,
        Instant sentAt
) {
}
