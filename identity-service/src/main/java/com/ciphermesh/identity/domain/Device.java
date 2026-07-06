package com.ciphermesh.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

/**
 * A registered device belonging to a user. In the Signal model a single user
 * may own several devices, each with its own long-term identity key. The
 * identity key stored here is the <em>public</em> half only — the private key
 * never leaves the client.
 */
@Entity
@Table(
        name = "device",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_device_user_device",
                columnNames = {"user_id", "device_id"}
        )
)
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "device_id", nullable = false)
    private int deviceId;

    @Column(name = "registration_id", nullable = false)
    private int registrationId;

    @Column(name = "identity_key", nullable = false)
    private byte[] identityKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Device() {
        // for JPA
    }

    public Device(String userId, int deviceId, int registrationId, byte[] identityKey) {
        this.userId = userId;
        this.deviceId = deviceId;
        this.registrationId = registrationId;
        this.identityKey = identityKey;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public int getDeviceId() {
        return deviceId;
    }

    public int getRegistrationId() {
        return registrationId;
    }

    public byte[] getIdentityKey() {
        return identityKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
