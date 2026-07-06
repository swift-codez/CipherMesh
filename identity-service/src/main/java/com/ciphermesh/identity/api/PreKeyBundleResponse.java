package com.ciphermesh.identity.api;

import com.ciphermesh.identity.domain.Device;
import com.ciphermesh.identity.domain.OneTimePreKey;
import com.ciphermesh.identity.domain.SignedPreKey;

import java.util.Base64;

/**
 * The public material a peer needs to run X3DH against a device: the identity
 * key, the current signed pre-key, and at most one one-time pre-key. The
 * one-time pre-key is {@code null} when the device's pool is exhausted, in
 * which case X3DH proceeds without it. All key bytes are base64-encoded.
 */
public record PreKeyBundleResponse(
        String userId,
        int deviceId,
        int registrationId,
        String identityKey,
        SignedPreKeyView signedPreKey,
        OneTimePreKeyView oneTimePreKey
) {

    public record SignedPreKeyView(int keyId, String publicKey, String signature) {
    }

    public record OneTimePreKeyView(int keyId, String publicKey) {
    }

    public static PreKeyBundleResponse from(Device device, SignedPreKey signedPreKey, OneTimePreKey oneTimePreKey) {
        Base64.Encoder encoder = Base64.getEncoder();
        SignedPreKeyView signedView = new SignedPreKeyView(
                signedPreKey.getKeyId(),
                encoder.encodeToString(signedPreKey.getPublicKey()),
                encoder.encodeToString(signedPreKey.getSignature()));
        OneTimePreKeyView oneTimeView = oneTimePreKey == null ? null : new OneTimePreKeyView(
                oneTimePreKey.getKeyId(),
                encoder.encodeToString(oneTimePreKey.getPublicKey()));
        return new PreKeyBundleResponse(
                device.getUserId(),
                device.getDeviceId(),
                device.getRegistrationId(),
                encoder.encodeToString(device.getIdentityKey()),
                signedView,
                oneTimeView);
    }
}
