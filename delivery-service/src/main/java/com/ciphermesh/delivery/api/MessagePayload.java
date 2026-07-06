package com.ciphermesh.delivery.api;

import com.ciphermesh.events.MessageEnvelopeEvent;

import java.time.Instant;

/**
 * The message shape pushed to a connected client. Contains only what the
 * recipient needs to decrypt and display — the opaque ciphertext and its
 * provenance. Internal routing fields are not exposed.
 */
public record MessagePayload(
        String messageId,
        String senderId,
        String ciphertext,
        Instant sentAt
) {

    public static MessagePayload from(MessageEnvelopeEvent event) {
        return new MessagePayload(event.messageId(), event.senderId(), event.ciphertext(), event.sentAt());
    }
}
