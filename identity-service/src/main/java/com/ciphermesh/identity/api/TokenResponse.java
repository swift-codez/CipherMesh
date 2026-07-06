package com.ciphermesh.identity.api;

/**
 * A signed access token and its lifetime in seconds.
 */
public record TokenResponse(String token, long expiresInSeconds) {
}
