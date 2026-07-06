package com.ciphermesh.identity.service;

import com.ciphermesh.events.PreKeysLowEvent;
import com.ciphermesh.identity.api.PreKeyBundleResponse;
import com.ciphermesh.identity.domain.Device;
import com.ciphermesh.identity.domain.OneTimePreKey;
import com.ciphermesh.identity.domain.SignedPreKey;
import com.ciphermesh.identity.messaging.IdentityEventPublisher;
import com.ciphermesh.identity.repository.DeviceRepository;
import com.ciphermesh.identity.repository.OneTimePreKeyRepository;
import com.ciphermesh.identity.repository.SignedPreKeyRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;

/**
 * Assembles the X3DH pre-key bundle a peer needs to start a session. Runs in a
 * single transaction so that claiming and deleting the one-time pre-key is
 * atomic: each key is handed out at most once, preserving forward secrecy.
 * When a consumed key drops the remaining pool below the configured watermark,
 * a {@code prekeys.low} event is emitted so the client replenishes.
 */
@Service
public class PreKeyBundleService {

    private final DeviceRepository deviceRepository;
    private final SignedPreKeyRepository signedPreKeyRepository;
    private final OneTimePreKeyRepository oneTimePreKeyRepository;
    private final IdentityEventPublisher eventPublisher;
    private final int lowWatermark;

    public PreKeyBundleService(DeviceRepository deviceRepository,
                               SignedPreKeyRepository signedPreKeyRepository,
                               OneTimePreKeyRepository oneTimePreKeyRepository,
                               IdentityEventPublisher eventPublisher,
                               @Value("${ciphermesh.identity.prekeys.low-watermark:10}") int lowWatermark) {
        this.deviceRepository = deviceRepository;
        this.signedPreKeyRepository = signedPreKeyRepository;
        this.oneTimePreKeyRepository = oneTimePreKeyRepository;
        this.eventPublisher = eventPublisher;
        this.lowWatermark = lowWatermark;
    }

    @Transactional
    public PreKeyBundleResponse assembleBundle(String userId) {
        Device device = deviceRepository.findByUserId(userId).stream()
                .min(Comparator.comparingInt(Device::getDeviceId))
                .orElseThrow(() -> new DeviceNotFoundException(userId));

        SignedPreKey signedPreKey = signedPreKeyRepository.findByDeviceAndActiveTrue(device)
                .orElseThrow(() -> new IllegalStateException(
                        "Registered device %s has no active signed pre-key".formatted(device.getId())));

        Optional<OneTimePreKey> oneTimePreKey =
                oneTimePreKeyRepository.claimNextForUpdate(device.getId());
        oneTimePreKey.ifPresent(claimed -> {
            oneTimePreKeyRepository.delete(claimed);
            notifyIfPoolLow(device);
        });

        return PreKeyBundleResponse.from(device, signedPreKey, oneTimePreKey.orElse(null));
    }

    private void notifyIfPoolLow(Device device) {
        long remaining = oneTimePreKeyRepository.countByDevice(device);
        if (remaining < lowWatermark) {
            eventPublisher.publishPreKeysLow(new PreKeysLowEvent(
                    device.getUserId(), device.getDeviceId(), remaining, Instant.now()));
        }
    }
}
