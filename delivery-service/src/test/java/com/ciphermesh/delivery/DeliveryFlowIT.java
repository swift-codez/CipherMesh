package com.ciphermesh.delivery;

import com.ciphermesh.delivery.mailbox.Mailbox;
import com.ciphermesh.delivery.messaging.InboundMessageProducer;
import com.ciphermesh.events.MessageEnvelopeEvent;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.awaitility.Awaitility.await;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end wiring test for the asynchronous delivery path. Publishing an
 * envelope for an offline recipient must flow through Kafka, the inbound
 * consumer, and the router, landing in the recipient's durable mailbox. Also
 * serves as a context-load smoke test for the full Spring/Redis/Kafka wiring.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
class DeliveryFlowIT {

    @Container
    @ServiceConnection
    static final ConfluentKafkaContainer KAFKA =
            new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.1"));

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private InboundMessageProducer producer;

    @Autowired
    private Mailbox mailbox;

    @Test
    void offlineRecipientMessageIsRoutedToMailbox() {
        String recipient = "offline-" + UUID.randomUUID();
        MessageEnvelopeEvent envelope = new MessageEnvelopeEvent(
                UUID.randomUUID().toString(), "alice", recipient, 1, "ciphertext", Instant.now());

        producer.publish(envelope);

        await().atMost(Duration.ofSeconds(30))
                .untilAsserted(() -> assertThat(mailbox.size(recipient, 1)).isEqualTo(1));
    }
}
