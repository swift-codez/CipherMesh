package com.ciphermesh.identity.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-that-is-at-least-32-bytes-long-0123456789";

    private final JwtService jwtService = new JwtService(SECRET, Duration.ofHours(1));

    @Test
    void issuesTokenThatRoundTripsToSubjectAndDeviceClaim() {
        String token = jwtService.issueToken("alice", 1);

        Claims claims = jwtService.parse(token);

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.get("deviceId", Integer.class)).isEqualTo(1);
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        String token = new JwtService("another-secret-that-is-also-32-bytes-long-9876543210", Duration.ofHours(1))
                .issueToken("bob", 2);

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jwtService.parse(token))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }
}
