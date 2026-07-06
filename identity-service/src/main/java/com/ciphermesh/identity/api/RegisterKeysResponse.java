package com.ciphermesh.identity.api;

import java.util.UUID;

/**
 * Confirmation returned after a successful registration: the server-assigned
 * device id and how many one-time pre-keys were accepted into the pool.
 */
public record RegisterKeysResponse(
        UUID id,
        int oneTimePreKeyCount
) {
}
