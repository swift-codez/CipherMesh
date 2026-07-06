package com.ciphermesh.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * A one-time pre-key for X3DH. Each device uploads a pool of these; the registry
 * hands exactly one to each requesting peer and then deletes it. Reusing a
 * one-time pre-key would weaken forward secrecy, so consumption must be atomic
 * and once-only (enforced at the repository layer in a later step).
 */
@Entity
@Table(
        name = "one_time_pre_key",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_otpk_device_key",
                columnNames = {"device_id", "key_id"}
        )
)
public class OneTimePreKey {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "key_id", nullable = false)
    private int keyId;

    @Column(name = "public_key", nullable = false)
    private byte[] publicKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected OneTimePreKey() {
        // for JPA
    }

    public OneTimePreKey(Device device, int keyId, byte[] publicKey) {
        this.device = device;
        this.keyId = keyId;
        this.publicKey = publicKey;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public Device getDevice() {
        return device;
    }

    public int getKeyId() {
        return keyId;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
