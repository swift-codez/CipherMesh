package com.ciphermesh.identity.repository;

import com.ciphermesh.identity.domain.Device;
import com.ciphermesh.identity.domain.OneTimePreKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the consume-once contract of {@code claimNextForUpdate}: keys are
 * handed out in order and each is deleted so it can never be issued twice.
 * Runs against real PostgreSQL because the query uses {@code FOR UPDATE SKIP
 * LOCKED}, which an in-memory database cannot honour.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class OneTimePreKeyConsumptionTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private OneTimePreKeyRepository oneTimePreKeyRepository;

    @Test
    void claimsEachKeyOnceThenReportsEmpty() {
        Device device = deviceRepository.save(new Device("erin", 1, 5, new byte[]{0}));
        oneTimePreKeyRepository.saveAll(List.of(
                new OneTimePreKey(device, 1, new byte[]{10}),
                new OneTimePreKey(device, 2, new byte[]{20})));

        int first = claimAndDelete(device);
        int second = claimAndDelete(device);

        assertThat(first).isEqualTo(1);
        assertThat(second).isEqualTo(2);
        assertThat(first).isNotEqualTo(second);
        assertThat(oneTimePreKeyRepository.claimNextForUpdate(device.getId())).isEmpty();
        assertThat(oneTimePreKeyRepository.countByDevice(device)).isZero();
    }

    private int claimAndDelete(Device device) {
        Optional<OneTimePreKey> claimed = oneTimePreKeyRepository.claimNextForUpdate(device.getId());
        assertThat(claimed).isPresent();
        oneTimePreKeyRepository.delete(claimed.get());
        oneTimePreKeyRepository.flush();
        return claimed.get().getKeyId();
    }
}
