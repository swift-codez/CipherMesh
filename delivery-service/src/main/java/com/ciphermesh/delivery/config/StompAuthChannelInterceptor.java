package com.ciphermesh.delivery.config;

import com.ciphermesh.delivery.security.JwtService;
import com.ciphermesh.delivery.security.JwtService.AuthenticatedUser;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

/**
 * Authenticates the STOMP CONNECT frame with the bearer JWT issued by the
 * identity service. The verified subject becomes the session
 * {@link java.security.Principal}, and the user/device are stored as session
 * attributes for the connect/disconnect listener. Identity therefore comes from
 * a signed token, never from client-supplied plain headers.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    public static final String USER_ID = "userId";
    public static final String DEVICE_ID = "deviceId";
    private static final String AUTHORIZATION = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;

    public StompAuthChannelInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authorization = accessor.getFirstNativeHeader(AUTHORIZATION);
            if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
                throw new IllegalArgumentException("CONNECT requires a bearer token");
            }
            AuthenticatedUser user = jwtService.verify(authorization.substring(BEARER_PREFIX.length()));
            accessor.setUser(new StompPrincipal(user.userId()));
            if (accessor.getSessionAttributes() != null) {
                accessor.getSessionAttributes().put(USER_ID, user.userId());
                accessor.getSessionAttributes().put(DEVICE_ID, user.deviceId());
            }
        }
        return message;
    }
}
