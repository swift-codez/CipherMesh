package com.ciphermesh.identity.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.List;

/**
 * A device's full X3DH key bundle uploaded at registration. All key material is
 * base64-encoded and public — the client retains every private key locally. The
 * one-time pre-key list may be empty; X3DH degrades gracefully to identity +
 * signed pre-key when the pool is exhausted.
 */
public record RegisterKeysRequest(
        @NotBlank String userId,
        @PositiveOrZero int deviceId,
        @PositiveOrZero int registrationId,
        @NotBlank String identityKey,
        @NotNull @Valid SignedPreKeyRequest signedPreKey,
        @NotNull List<@Valid OneTimePreKeyRequest> oneTimePreKeys
) {
}
