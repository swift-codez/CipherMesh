package com.ciphermesh.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * The public half of a device's signed pre-key. {@code publicKey} and
 * {@code signature} are base64-encoded; the signature is produced by the
 * device's identity key so requesters can verify authenticity client-side.
 */
public record SignedPreKeyRequest(
        @PositiveOrZero int keyId,
        @NotBlank String publicKey,
        @NotBlank String signature
) {
}
