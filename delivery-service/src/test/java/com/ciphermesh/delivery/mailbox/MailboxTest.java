package com.ciphermesh.delivery.mailbox;

import com.ciphermesh.events.MessageEnvelopeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies durable store-and-forward: envelopes are queued in order, drained
 * exactly once (FIFO), and a second drain returns nothing so a reconnect cannot
 * redeliver already-drained messages.
 */
@Testcontainers
class MailboxTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static StringRedisTemplate redisTemplate;
    private Mailbox mailbox;

    @BeforeAll
    static void connect() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(REDIS.getHost(), REDIS.getMappedPort(6379)));
        factory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(factory);
        redisTemplate.afterPropertiesSet();
    }

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mailbox = new Mailbox(redisTemplate, objectMapper);
    }

    @Test
    void storesDrainsInOrderAndIsEmptyAfterwards() {
        mailbox.store(envelope("bob", 1, "m1"));
        mailbox.store(envelope("bob", 1, "m2"));

        assertThat(mailbox.size("bob", 1)).isEqualTo(2);

        List<MessageEnvelopeEvent> drained = mailbox.drain("bob", 1);

        assertThat(drained).extracting(MessageEnvelopeEvent::messageId).containsExactly("m1", "m2");
        assertThat(mailbox.size("bob", 1)).isZero();
        assertThat(mailbox.drain("bob", 1)).isEmpty();
    }

    @Test
    void isolatesMailboxesPerDevice() {
        mailbox.store(envelope("erin", 1, "a"));
        mailbox.store(envelope("erin", 2, "b"));

        assertThat(mailbox.drain("erin", 1)).extracting(MessageEnvelopeEvent::messageId).containsExactly("a");
        assertThat(mailbox.drain("erin", 2)).extracting(MessageEnvelopeEvent::messageId).containsExactly("b");
    }

    private static MessageEnvelopeEvent envelope(String recipient, int deviceId, String messageId) {
        return new MessageEnvelopeEvent(messageId, "alice", recipient, deviceId, "cipher", Instant.now());
    }
}
