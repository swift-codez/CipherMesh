package com.ciphermesh.identity.repository;

import com.ciphermesh.identity.domain.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByUserIdAndDeviceId(String userId, int deviceId);

    boolean existsByUserIdAndDeviceId(String userId, int deviceId);

    List<Device> findByUserId(String userId);
}
