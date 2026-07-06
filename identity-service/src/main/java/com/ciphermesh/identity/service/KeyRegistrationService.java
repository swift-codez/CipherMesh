package com.ciphermesh.identity.service;

import com.ciphermesh.identity.api.OneTimePreKeyRequest;
import com.ciphermesh.identity.api.RegisterKeysRequest;
import com.ciphermesh.identity.api.RegisterKeysResponse;
import com.ciphermesh.identity.api.SignedPreKeyRequest;
import com.ciphermesh.events.UserRegisteredEvent;
import com.ciphermesh.identity.domain.Device;
import com.ciphermesh.identity.domain.OneTimePreKey;
import com.ciphermesh.identity.domain.SignedPreKey;
import com.ciphermesh.identity.messaging.IdentityEventPublisher;
import com.ciphermesh.identity.repository.DeviceRepository;
import com.ciphermesh.identity.repository.OneTimePreKeyRepository;
import com.ciphermesh.identity.repository.SignedPreKeyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Base64;
import java.util.List;

/**
 * Persists a device's public X3DH key bundle. The whole registration is one
 * transaction: identity key, the active signed pre-key, and the one-time
 * pre-key pool are committed together or not at all.
 */
@Service
public class KeyRegistrationService {

    private final DeviceRepository deviceRepository;
    private final SignedPreKeyRepository signedPreKeyRepository;
    private final OneTimePreKeyRepository oneTimePreKeyRepository;
    private final IdentityEventPublisher eventPublisher;

    public KeyRegistrationService(DeviceRepository deviceRepository,
                                  SignedPreKeyRepository signedPreKeyRepository,
                                  OneTimePreKeyRepository oneTimePreKeyRepository,
                                  IdentityEventPublisher eventPublisher) {
        this.deviceRepository = deviceRepository;
        this.signedPreKeyRepository = signedPreKeyRepository;
        this.oneTimePreKeyRepository = oneTimePreKeyRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public RegisterKeysResponse register(RegisterKeysRequest request) {
        if (deviceRepository.existsByUserIdAndDeviceId(request.userId(), request.deviceId())) {
            throw new DeviceAlreadyRegisteredException(request.userId(), request.deviceId());
        }

        Device device = deviceRepository.save(new Device(
                request.userId(),
                request.deviceId(),
                request.registrationId(),
                decode(request.identityKey(), "identityKey")));

        SignedPreKeyRequest spk = request.signedPreKey();
        signedPreKeyRepository.save(new SignedPreKey(
                device,
                spk.keyId(),
                decode(spk.publicKey(), "signedPreKey.publicKey"),
                decode(spk.signature(), "signedPreKey.signature"),
                true));

        List<OneTimePreKey> oneTimePreKeys = request.oneTimePreKeys().stream()
                .map(otpk -> toEntity(device, otpk))
                .toList();
        oneTimePreKeyRepository.saveAll(oneTimePreKeys);

        eventPublisher.publishUserRegistered(new UserRegisteredEvent(
                device.getUserId(), device.getDeviceId(), device.getRegistrationId(), Instant.now()));

        return new RegisterKeysResponse(device.getId(), oneTimePreKeys.size());
    }

    private static OneTimePreKey toEntity(Device device, OneTimePreKeyRequest request) {
        return new OneTimePreKey(device, request.keyId(),
                decode(request.publicKey(), "oneTimePreKeys[].publicKey"));
    }

    private static byte[] decode(String base64, String field) {
        try {
            return Base64.getDecoder().decode(base64);
        } catch (IllegalArgumentException ex) {
            throw new InvalidKeyMaterialException(field);
        }
    }
}
