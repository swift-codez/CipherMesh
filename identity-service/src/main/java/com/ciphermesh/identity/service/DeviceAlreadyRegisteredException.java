package com.ciphermesh.identity.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Raised when a (userId, deviceId) pair is already registered. Re-registration
 * would clobber existing key material and break in-flight sessions, so it is
 * rejected with 409 Conflict.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DeviceAlreadyRegisteredException extends RuntimeException {

    public DeviceAlreadyRegisteredException(String userId, int deviceId) {
        super("Device already registered for user '%s' device %d".formatted(userId, deviceId));
    }
}
