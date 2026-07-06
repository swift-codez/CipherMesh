package com.ciphermesh.identity.repository;

import com.ciphermesh.identity.domain.Device;
import com.ciphermesh.identity.domain.SignedPreKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SignedPreKeyRepository extends JpaRepository<SignedPreKey, UUID> {

    Optional<SignedPreKey> findByDeviceAndActiveTrue(Device device);
}
