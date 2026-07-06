package com.ciphermesh.delivery.mailbox;

import com.ciphermesh.events.MessageEnvelopeEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Durable per-device store-and-forward queue for recipients that are offline.
 * Encrypted envelopes are appended to a Redis list ({@code cm:mailbox:<userId>:<deviceId>})
 * and drained on reconnect. Draining uses atomic left-pops so a message is
 * removed exactly once even if two drains race, preventing duplicate delivery.
 */
@Component
public class Mailbox {

    private static final String KEY_PREFIX = "cm:mailbox:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public Mailbox(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public void store(MessageEnvelopeEvent event) {
        redis.opsForList().rightPush(key(event.recipientId(), event.recipientDeviceId()), toJson(event));
    }

    public List<MessageEnvelopeEvent> drain(String userId, int deviceId) {
        String key = key(userId, deviceId);
        List<MessageEnvelopeEvent> drained = new ArrayList<>();
        String json;
        while ((json = redis.opsForList().leftPop(key)) != null) {
            drained.add(fromJson(json));
        }
        return drained;
    }

    public long size(String userId, int deviceId) {
        Long size = redis.opsForList().size(key(userId, deviceId));
        return size == null ? 0 : size;
    }

    private static String key(String userId, int deviceId) {
        return KEY_PREFIX + userId + ":" + deviceId;
    }

    private String toJson(MessageEnvelopeEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Unable to serialize message envelope", ex);
        }
    }

    private MessageEnvelopeEvent fromJson(String json) {
        try {
            return objectMapper.readValue(json, MessageEnvelopeEvent.class);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Corrupt mailbox entry", ex);
        }
    }
}
