package com.ciphermesh.delivery.routing;

import com.ciphermesh.events.MessageEnvelopeEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Cross-instance hand-off over Redis pub/sub. When a recipient is connected to a
 * different instance, the routing instance publishes the envelope to that
 * instance's channel ({@code cm:instance:<instanceId>}); the owning instance
 * receives it here and delivers it over the local WebSocket.
 */
@Component
public class InstanceMessageChannel implements MessageListener {

    static final String CHANNEL_PREFIX = "cm:instance:";

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final LocalDeliveryService localDeliveryService;

    public InstanceMessageChannel(StringRedisTemplate redis,
                                  ObjectMapper objectMapper,
                                  LocalDeliveryService localDeliveryService) {
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.localDeliveryService = localDeliveryService;
    }

    public static String channelFor(String instanceId) {
        return CHANNEL_PREFIX + instanceId;
    }

    public void forward(String instanceId, MessageEnvelopeEvent event) {
        redis.convertAndSend(channelFor(instanceId), toJson(event));
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String json = new String(message.getBody(), StandardCharsets.UTF_8);
        localDeliveryService.deliver(fromJson(json));
    }

    private String toJson(MessageEnvelopeEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to serialize message envelope", ex);
        }
    }

    private MessageEnvelopeEvent fromJson(String json) {
        try {
            return objectMapper.readValue(json, MessageEnvelopeEvent.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Corrupt instance-channel message", ex);
        }
    }
}
