package com.ciphermesh.identity.repository;

import com.ciphermesh.identity.domain.Device;
import com.ciphermesh.identity.domain.OneTimePreKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OneTimePreKeyRepository extends JpaRepository<OneTimePreKey, UUID> {

    long countByDevice(Device device);

    List<OneTimePreKey> findByDevice(Device device);
}
