package com.ciphermesh.identity.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Raised when a pre-key bundle is requested for a user that has no registered
 * device. Rejected with 404 Not Found.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class DeviceNotFoundException extends RuntimeException {

    public DeviceNotFoundException(String userId) {
        super("No registered device found for user '%s'".formatted(userId));
    }
}
