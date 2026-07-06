package com.ciphermesh.identity.repository;

import com.ciphermesh.identity.domain.Device;
import com.ciphermesh.identity.domain.OneTimePreKey;
import com.ciphermesh.identity.domain.SignedPreKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Repository slice test backed by a real PostgreSQL instance via Testcontainers,
 * so finders, constraints, and {@code byte[]} -> bytea mapping are exercised
 * against the database we actually deploy on rather than an in-memory stand-in.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class DeviceRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void hibernateSettings(DynamicPropertyRegistry registry) {
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "create-drop");
    }

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private SignedPreKeyRepository signedPreKeyRepository;

    @Autowired
    private OneTimePreKeyRepository oneTimePreKeyRepository;

    @Test
    void persistsAndFindsDeviceByUserAndDeviceId() {
        Device saved = deviceRepository.save(
                new Device("alice", 1, 42, new byte[]{1, 2, 3}));

        Optional<Device> found = deviceRepository.findByUserIdAndDeviceId("alice", 1);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(saved.getId());
        assertThat(found.get().getRegistrationId()).isEqualTo(42);
        assertThat(found.get().getIdentityKey()).containsExactly(1, 2, 3);
        assertThat(found.get().getCreatedAt()).isNotNull();
    }

    @Test
    void reportsDeviceExistence() {
        deviceRepository.save(new Device("bob", 1, 7, new byte[]{9}));

        assertThat(deviceRepository.existsByUserIdAndDeviceId("bob", 1)).isTrue();
        assertThat(deviceRepository.existsByUserIdAndDeviceId("bob", 2)).isFalse();
    }

    @Test
    void findsActiveSignedPreKeyForDevice() {
        Device device = deviceRepository.save(new Device("carol", 1, 11, new byte[]{0}));
        signedPreKeyRepository.save(
                new SignedPreKey(device, 100, new byte[]{4, 5}, new byte[]{6, 7}, false));
        SignedPreKey active = signedPreKeyRepository.save(
                new SignedPreKey(device, 101, new byte[]{8, 9}, new byte[]{10}, true));

        Optional<SignedPreKey> found = signedPreKeyRepository.findByDeviceAndActiveTrue(device);

        assertThat(found).isPresent();
        assertThat(found.get().getKeyId()).isEqualTo(active.getKeyId());
    }

    @Test
    void countsOneTimePreKeysInPool() {
        Device device = deviceRepository.save(new Device("dave", 1, 13, new byte[]{0}));
        oneTimePreKeyRepository.saveAll(List.of(
                new OneTimePreKey(device, 1, new byte[]{1}),
                new OneTimePreKey(device, 2, new byte[]{2}),
                new OneTimePreKey(device, 3, new byte[]{3})));

        assertThat(oneTimePreKeyRepository.countByDevice(device)).isEqualTo(3);
        assertThat(oneTimePreKeyRepository.findByDevice(device)).hasSize(3);
    }
}
