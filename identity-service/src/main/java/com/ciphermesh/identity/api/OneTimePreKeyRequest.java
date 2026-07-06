package com.ciphermesh.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * A single base64-encoded one-time pre-key uploaded as part of a device's
 * bundle. The server hands each of these to exactly one requesting peer.
 */
public record OneTimePreKeyRequest(
        @PositiveOrZero int keyId,
        @NotBlank String publicKey
) {
}
