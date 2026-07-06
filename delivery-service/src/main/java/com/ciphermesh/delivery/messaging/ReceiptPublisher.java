package com.ciphermesh.delivery.messaging;

import com.ciphermesh.events.MessageReceiptEvent;
import com.ciphermesh.events.Topics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Emits delivery and read receipts to Kafka so senders can be notified
 * asynchronously without the delivery service holding that state.
 */
@Component
public class ReceiptPublisher {

    public static final String DELIVERED = "DELIVERED";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public ReceiptPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishDelivered(String messageId, String byUserId) {
        MessageReceiptEvent receipt =
                new MessageReceiptEvent(messageId, byUserId, DELIVERED, Instant.now());
        kafkaTemplate.send(Topics.MESSAGES_RECEIPTS, messageId, receipt);
    }
}
