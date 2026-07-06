package com.ciphermesh.delivery.api;

/**
 * Payload a client sends to {@code /app/send}. {@code ciphertext} is the
 * Double Ratchet output produced on the sender's device; the server treats it
 * as opaque.
 */
public record SendMessageRequest(
        String recipientId,
        int recipientDeviceId,
        String ciphertext
) {
}
