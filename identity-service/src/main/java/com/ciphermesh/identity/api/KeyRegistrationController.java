package com.ciphermesh.identity.api;

import com.ciphermesh.identity.service.KeyRegistrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/keys")
public class KeyRegistrationController {

    private final KeyRegistrationService keyRegistrationService;

    public KeyRegistrationController(KeyRegistrationService keyRegistrationService) {
        this.keyRegistrationService = keyRegistrationService;
    }

    /**
     * Registers a device and its public X3DH key bundle.
     *
     * @return 201 Created with the assigned device id and accepted pre-key count
     */
    @PostMapping
    public ResponseEntity<RegisterKeysResponse> register(@Valid @RequestBody RegisterKeysRequest request) {
        RegisterKeysResponse response = keyRegistrationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
