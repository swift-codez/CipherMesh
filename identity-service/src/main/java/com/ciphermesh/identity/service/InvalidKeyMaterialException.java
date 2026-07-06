package com.ciphermesh.identity.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Raised when submitted key material is not valid base64 and therefore cannot
 * be decoded into bytes. Rejected with 400 Bad Request.
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidKeyMaterialException extends RuntimeException {

    public InvalidKeyMaterialException(String field) {
        super("Field '%s' is not valid base64-encoded key material".formatted(field));
    }
}
