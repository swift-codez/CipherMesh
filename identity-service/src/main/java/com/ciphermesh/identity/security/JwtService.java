package com.ciphermesh.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * Issues and verifies the HS256 JWTs that authenticate a user/device to the
 * delivery service. The signing secret is shared between the identity and
 * delivery services out of band (configuration/secret manager).
 */
@Service
public class JwtService {

    private static final String DEVICE_ID_CLAIM = "deviceId";

    private final SecretKey key;
    private final Duration ttl;

    public JwtService(@Value("${ciphermesh.jwt.secret}") String secret,
                      @Value("${ciphermesh.jwt.ttl:PT24H}") Duration ttl) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttl = ttl;
    }

    public String issueToken(String userId, int deviceId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim(DEVICE_ID_CLAIM, deviceId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(ttl)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    public long ttlSeconds() {
        return ttl.toSeconds();
    }
}
