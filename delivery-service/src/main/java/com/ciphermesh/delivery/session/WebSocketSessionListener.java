package com.ciphermesh.delivery.session;

import com.ciphermesh.delivery.config.StompAuthChannelInterceptor;
import com.ciphermesh.delivery.mailbox.Mailbox;
import com.ciphermesh.delivery.routing.LocalDeliveryService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Map;

/**
 * Keeps the session registry in sync with live WebSocket sessions and drains a
 * device's mailbox when it (re)connects.
 *
 * <p>The session is registered <em>before</em> draining so that any message
 * arriving during reconnect is routed to the live session rather than the
 * mailbox; combined with the mailbox's atomic pops, this prevents a message
 * from being delivered twice.
 */
@Component
public class WebSocketSessionListener {

    private final SessionRegistry sessionRegistry;
    private final InstanceIdentity instanceIdentity;
    private final Mailbox mailbox;
    private final LocalDeliveryService localDeliveryService;

    public WebSocketSessionListener(SessionRegistry sessionRegistry,
                                    InstanceIdentity instanceIdentity,
                                    Mailbox mailbox,
                                    LocalDeliveryService localDeliveryService) {
        this.sessionRegistry = sessionRegistry;
        this.instanceIdentity = instanceIdentity;
        this.mailbox = mailbox;
        this.localDeliveryService = localDeliveryService;
    }

    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        withIdentity(event.getMessage(), (userId, deviceId) -> {
            sessionRegistry.register(userId, deviceId, instanceIdentity.id());
            mailbox.drain(userId, deviceId).forEach(localDeliveryService::deliver);
        });
    }

    @EventListener
    public void onSessionDisconnect(SessionDisconnectEvent event) {
        withIdentity(event.getMessage(), sessionRegistry::remove);
    }

    private void withIdentity(org.springframework.messaging.Message<?> message, IdentityConsumer consumer) {
        Map<String, Object> attributes = StompHeaderAccessor.wrap(message).getSessionAttributes();
        if (attributes == null) {
            return;
        }
        String userId = (String) attributes.get(StompAuthChannelInterceptor.USER_ID);
        Integer deviceId = (Integer) attributes.get(StompAuthChannelInterceptor.DEVICE_ID);
        if (userId != null && deviceId != null) {
            consumer.accept(userId, deviceId);
        }
    }

    @FunctionalInterface
    private interface IdentityConsumer {
        void accept(String userId, int deviceId);
    }
}
