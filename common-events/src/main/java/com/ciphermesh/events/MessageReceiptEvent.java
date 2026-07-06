package com.ciphermesh.events;

import java.time.Instant;

/**
 * Emitted by the delivery service when a message reaches or is acknowledged by
 * a recipient. Carries no plaintext — only the message id, who acknowledged it,
 * and the receipt status (e.g. {@code DELIVERED}, {@code READ}).
 */
public record MessageReceiptEvent(
        String messageId,
        String byUserId,
        String status,
        Instant occurredAt
) {
}
