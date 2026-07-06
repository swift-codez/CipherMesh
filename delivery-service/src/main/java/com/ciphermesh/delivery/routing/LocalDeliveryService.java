package com.ciphermesh.delivery.routing;

import com.ciphermesh.delivery.api.MessagePayload;
import com.ciphermesh.events.MessageEnvelopeEvent;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

/**
 * Pushes a message to a recipient connected to THIS instance over their STOMP
 * user destination. Used both for live delivery and for draining the mailbox on
 * reconnect.
 */
@Service
public class LocalDeliveryService {

    static final String USER_DESTINATION = "/queue/messages";

    private final SimpMessagingTemplate messagingTemplate;

    public LocalDeliveryService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void deliver(MessageEnvelopeEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.recipientId(), USER_DESTINATION, MessagePayload.from(event));
    }
}
