package com.ciphermesh.delivery.session;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Tracks which delivery-service instance each connected device is attached to.
 * Backed by a Redis hash per user ({@code cm:sessions:<userId>}) mapping a
 * device id to an instance id, so any instance can discover where a recipient
 * is currently connected.
 */
@Component
public class SessionRegistry {

    private static final String KEY_PREFIX = "cm:sessions:";

    private final StringRedisTemplate redis;

    public SessionRegistry(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public void register(String userId, int deviceId, String instanceId) {
        redis.opsForHash().put(key(userId), field(deviceId), instanceId);
    }

    public void remove(String userId, int deviceId) {
        redis.opsForHash().delete(key(userId), field(deviceId));
    }

    public Optional<String> findInstance(String userId, int deviceId) {
        Object instanceId = redis.opsForHash().get(key(userId), field(deviceId));
        return Optional.ofNullable((String) instanceId);
    }

    public boolean isOnline(String userId, int deviceId) {
        return findInstance(userId, deviceId).isPresent();
    }

    private static String key(String userId) {
        return KEY_PREFIX + userId;
    }

    private static String field(int deviceId) {
        return Integer.toString(deviceId);
    }
}
