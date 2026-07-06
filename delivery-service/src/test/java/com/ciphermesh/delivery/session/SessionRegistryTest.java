package com.ciphermesh.delivery.session;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the Redis-backed session registry against a real Redis instance.
 */
@Testcontainers
class SessionRegistryTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private static StringRedisTemplate redisTemplate;
    private SessionRegistry registry;

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
        registry = new SessionRegistry(redisTemplate);
    }

    @Test
    void registersAndFindsInstanceForDevice() {
        registry.register("alice", 1, "instance-A");

        Optional<String> instance = registry.findInstance("alice", 1);

        assertThat(instance).contains("instance-A");
        assertThat(registry.isOnline("alice", 1)).isTrue();
    }

    @Test
    void reportsOfflineForUnknownOrRemovedDevice() {
        registry.register("carol", 3, "instance-A");
        registry.remove("carol", 3);

        assertThat(registry.findInstance("carol", 3)).isEmpty();
        assertThat(registry.isOnline("carol", 99)).isFalse();
    }

    @Test
    void tracksDevicesOfSameUserIndependently() {
        registry.register("dave", 1, "instance-A");
        registry.register("dave", 2, "instance-B");

        assertThat(registry.findInstance("dave", 1)).contains("instance-A");
        assertThat(registry.findInstance("dave", 2)).contains("instance-B");
    }
}
