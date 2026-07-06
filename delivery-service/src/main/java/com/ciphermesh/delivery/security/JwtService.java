package com.ciphermesh.delivery.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

/**
 * Verifies the HS256 access tokens issued by the identity service and extracts
 * the authenticated user/device. Shares the signing secret with the issuer.
 */
@Service
public class JwtService {

    private static final String DEVICE_ID_CLAIM = "deviceId";

    private final SecretKey key;

    public JwtService(@Value("${ciphermesh.jwt.secret}") String secret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public AuthenticatedUser verify(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return new AuthenticatedUser(claims.getSubject(), claims.get(DEVICE_ID_CLAIM, Integer.class));
    }

    public record AuthenticatedUser(String userId, int deviceId) {
    }
}
