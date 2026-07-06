package com.ciphermesh.delivery.routing;

import com.ciphermesh.delivery.mailbox.Mailbox;
import com.ciphermesh.delivery.session.InstanceIdentity;
import com.ciphermesh.delivery.session.SessionRegistry;
import com.ciphermesh.events.MessageEnvelopeEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageRouterTest {

    @Mock
    private SessionRegistry sessionRegistry;
    @Mock
    private InstanceIdentity instanceIdentity;
    @Mock
    private LocalDeliveryService localDeliveryService;
    @Mock
    private InstanceMessageChannel instanceMessageChannel;
    @Mock
    private Mailbox mailbox;

    private MessageRouter router() {
        return new MessageRouter(sessionRegistry, instanceIdentity,
                localDeliveryService, instanceMessageChannel, mailbox);
    }

    private static MessageEnvelopeEvent envelopeTo(String recipient, int deviceId) {
        return new MessageEnvelopeEvent("m1", "alice", recipient, deviceId, "cipher", Instant.now());
    }

    @Test
    void storesInMailboxWhenRecipientOffline() {
        MessageEnvelopeEvent event = envelopeTo("bob", 1);
        when(sessionRegistry.findInstance("bob", 1)).thenReturn(Optional.empty());

        router().route(event);

        verify(mailbox).store(event);
        verifyNoInteractions(localDeliveryService);
        verifyNoInteractions(instanceMessageChannel);
    }

    @Test
    void deliversLocallyWhenRecipientOnThisInstance() {
        MessageEnvelopeEvent event = envelopeTo("bob", 1);
        when(sessionRegistry.findInstance("bob", 1)).thenReturn(Optional.of("instance-A"));
        when(instanceIdentity.id()).thenReturn("instance-A");

        router().route(event);

        verify(localDeliveryService).deliver(event);
        verify(mailbox, never()).store(event);
        verifyNoInteractions(instanceMessageChannel);
    }

    @Test
    void forwardsToOwningInstanceWhenRecipientElsewhere() {
        MessageEnvelopeEvent event = envelopeTo("bob", 1);
        when(sessionRegistry.findInstance("bob", 1)).thenReturn(Optional.of("instance-B"));
        when(instanceIdentity.id()).thenReturn("instance-A");

        router().route(event);

        verify(instanceMessageChannel).forward("instance-B", event);
        verifyNoInteractions(localDeliveryService);
        verify(mailbox, never()).store(event);
    }
}
