package com.ciphermesh.identity.api;

import com.ciphermesh.identity.security.JwtService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final JwtService jwtService;

    public AuthController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    /**
     * Issues a signed access token the client presents when connecting to the
     * delivery service's WebSocket.
     */
    @PostMapping("/token")
    public TokenResponse token(@Valid @RequestBody TokenRequest request) {
        String token = jwtService.issueToken(request.userId(), request.deviceId());
        return new TokenResponse(token, jwtService.ttlSeconds());
    }
}
