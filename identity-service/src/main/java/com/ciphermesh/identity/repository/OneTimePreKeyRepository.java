package com.ciphermesh.identity.repository;

import com.ciphermesh.identity.domain.Device;
import com.ciphermesh.identity.domain.OneTimePreKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OneTimePreKeyRepository extends JpaRepository<OneTimePreKey, UUID> {

    long countByDevice(Device device);

    List<OneTimePreKey> findByDevice(Device device);

    /**
     * Atomically claims one available one-time pre-key for a device. The row is
     * locked with {@code FOR UPDATE} so no other transaction can hand out the
     * same key, and {@code SKIP LOCKED} lets concurrent bundle requests grab
     * different keys instead of blocking. The caller deletes the returned key
     * within the same transaction, guaranteeing each is issued at most once.
     */
    @Query(value = """
            SELECT * FROM one_time_pre_key
            WHERE device_id = :deviceId
            ORDER BY key_id
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """, nativeQuery = true)
    Optional<OneTimePreKey> claimNextForUpdate(@Param("deviceId") UUID deviceId);
}
