package com.ciphermesh.identity.messaging;

import com.ciphermesh.events.PreKeysLowEvent;
import com.ciphermesh.events.Topics;
import com.ciphermesh.events.UserRegisteredEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes identity-domain events to Kafka. The user id is used as the message
 * key so all events for a user land on the same partition and preserve ordering.
 */
@Component
public class IdentityEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public IdentityEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publishUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send(Topics.USER_REGISTERED, event.userId(), event);
    }

    public void publishPreKeysLow(PreKeysLowEvent event) {
        kafkaTemplate.send(Topics.PREKEYS_LOW, event.userId(), event);
    }
}
