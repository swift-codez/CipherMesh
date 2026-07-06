package com.ciphermesh.identity.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request for a delivery-service access token. In this reference implementation
 * the identity is asserted directly; a production system would first
 * authenticate the caller (password, passkey, OAuth) before issuing a token.
 */
public record TokenRequest(
        @NotBlank String userId,
        @PositiveOrZero int deviceId
) {
}
