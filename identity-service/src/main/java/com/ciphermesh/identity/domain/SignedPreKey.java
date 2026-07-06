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

import java.time.Instant;
import java.util.UUID;

/**
 * A medium-term signed pre-key for X3DH. It is signed by the owning device's
 * identity key so a requester can verify authenticity. A device keeps exactly
 * one {@code active} signed pre-key at a time; rotation marks the previous one
 * inactive rather than deleting it, so in-flight sessions stay valid.
 */
@Entity
@Table(name = "signed_pre_key")
public class SignedPreKey {

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

    @Column(name = "signature", nullable = false)
    private byte[] signature;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SignedPreKey() {
        // for JPA
    }

    public SignedPreKey(Device device, int keyId, byte[] publicKey, byte[] signature, boolean active) {
        this.device = device;
        this.keyId = keyId;
        this.publicKey = publicKey;
        this.signature = signature;
        this.active = active;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
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

    public byte[] getSignature() {
        return signature;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
