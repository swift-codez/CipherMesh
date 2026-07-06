package com.ciphermesh.delivery.api;

/**
 * Payload a client sends to {@code /app/ack} to acknowledge receipt of a
 * message, triggering a delivery receipt back to the sender.
 */
public record AckRequest(String messageId) {
}
