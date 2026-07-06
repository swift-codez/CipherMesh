package com.ciphermesh.delivery.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * Reads identity from the STOMP CONNECT frame and attaches it to the session.
 * The {@code userId} becomes the session {@link java.security.Principal} and
 * both {@code userId} and {@code deviceId} are stored as session attributes so
 * the connect/disconnect listener can register and deregister sessions.
 *
 * <p>This is an interim stand-in for token-based authentication, which replaces
 * the trusted headers with a verified JWT in a later step.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    public static final String USER_ID = "userId";
    public static final String DEVICE_ID = "deviceId";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String userId = accessor.getFirstNativeHeader(USER_ID);
            String deviceId = accessor.getFirstNativeHeader(DEVICE_ID);
            if (userId == null || deviceId == null) {
                throw new IllegalArgumentException("CONNECT requires userId and deviceId headers");
            }
            accessor.setUser(new StompPrincipal(userId));
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put(USER_ID, userId);
                accessor.getSessionAttributes().put(DEVICE_ID, Integer.parseInt(deviceId));
            }
        }
        return message;
    }
}
