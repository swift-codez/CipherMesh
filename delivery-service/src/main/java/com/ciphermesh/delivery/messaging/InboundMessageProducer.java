package com.ciphermesh.delivery.messaging;

import com.ciphermesh.events.MessageEnvelopeEvent;
import com.ciphermesh.events.Topics;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes inbound encrypted envelopes to Kafka, decoupling ingestion (the
 * WebSocket SEND) from routing and delivery. Keyed by recipient id so a
 * recipient's messages preserve order on a single partition.
 */
@Component
public class InboundMessageProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public InboundMessageProducer(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(MessageEnvelopeEvent event) {
        kafkaTemplate.send(Topics.MESSAGES_INBOUND, event.recipientId(), event);
    }
}
