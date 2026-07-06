package com.ciphermesh.delivery.security;

import com.ciphermesh.delivery.security.JwtService.AuthenticatedUser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "shared-secret-that-is-at-least-32-bytes-long-0123456789";

    private final JwtService jwtService = new JwtService(SECRET);

    @Test
    void verifiesTokenAndExtractsUserAndDevice() {
        String token = Jwts.builder()
                .subject("bob")
                .claim("deviceId", 3)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();

        AuthenticatedUser user = jwtService.verify(token);

        assertThat(user.userId()).isEqualTo("bob");
        assertThat(user.deviceId()).isEqualTo(3);
    }

    @Test
    void rejectsTokenSignedWithDifferentSecret() {
        String token = Jwts.builder()
                .subject("bob")
                .claim("deviceId", 3)
                .signWith(Keys.hmacShaKeyFor("a-totally-different-secret-32-bytes-9876543210".getBytes(StandardCharsets.UTF_8)))
                .compact();

        assertThatThrownBy(() -> jwtService.verify(token))
                .isInstanceOf(io.jsonwebtoken.security.SignatureException.class);
    }
}
