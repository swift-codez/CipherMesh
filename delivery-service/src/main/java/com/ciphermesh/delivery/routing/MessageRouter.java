package com.ciphermesh.delivery.routing;

import com.ciphermesh.delivery.mailbox.Mailbox;
import com.ciphermesh.delivery.session.InstanceIdentity;
import com.ciphermesh.delivery.session.SessionRegistry;
import com.ciphermesh.events.MessageEnvelopeEvent;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Decides where an encrypted envelope goes based on the recipient's current
 * session:
 * <ul>
 *   <li>connected to this instance -&gt; deliver locally over WebSocket;</li>
 *   <li>connected to another instance -&gt; forward via the instance channel;</li>
 *   <li>offline -&gt; persist to the durable mailbox for later drain.</li>
 * </ul>
 * Because the server never holds keys or plaintext, the envelope is opaque at
 * every branch.
 */
@Service
public class MessageRouter {

    private final SessionRegistry sessionRegistry;
    private final InstanceIdentity instanceIdentity;
    private final LocalDeliveryService localDeliveryService;
    private final InstanceMessageChannel instanceMessageChannel;
    private final Mailbox mailbox;

    public MessageRouter(SessionRegistry sessionRegistry,
                         InstanceIdentity instanceIdentity,
                         LocalDeliveryService localDeliveryService,
                         InstanceMessageChannel instanceMessageChannel,
                         Mailbox mailbox) {
        this.sessionRegistry = sessionRegistry;
        this.instanceIdentity = instanceIdentity;
        this.localDeliveryService = localDeliveryService;
        this.instanceMessageChannel = instanceMessageChannel;
        this.mailbox = mailbox;
    }

    public void route(MessageEnvelopeEvent event) {
        Optional<String> instanceId =
                sessionRegistry.findInstance(event.recipientId(), event.recipientDeviceId());

        if (instanceId.isEmpty()) {
            mailbox.store(event);
        } else if (instanceId.get().equals(instanceIdentity.id())) {
            localDeliveryService.deliver(event);
        } else {
            instanceMessageChannel.forward(instanceId.get(), event);
        }
    }
}
