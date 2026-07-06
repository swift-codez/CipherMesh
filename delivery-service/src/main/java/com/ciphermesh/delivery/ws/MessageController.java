package com.ciphermesh.delivery.ws;

import com.ciphermesh.delivery.api.AckRequest;
import com.ciphermesh.delivery.api.SendMessageRequest;
import com.ciphermesh.delivery.messaging.InboundMessageProducer;
import com.ciphermesh.delivery.messaging.ReceiptPublisher;
import com.ciphermesh.events.MessageEnvelopeEvent;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entry point for client STOMP frames. Sending publishes an envelope to Kafka
 * for asynchronous routing; acknowledging emits a delivery receipt. The sender
 * identity comes from the authenticated session {@link Principal}, never from
 * the client-supplied payload.
 */
@Controller
public class MessageController {

    private final InboundMessageProducer inboundMessageProducer;
    private final ReceiptPublisher receiptPublisher;

    public MessageController(InboundMessageProducer inboundMessageProducer,
                             ReceiptPublisher receiptPublisher) {
        this.inboundMessageProducer = inboundMessageProducer;
        this.receiptPublisher = receiptPublisher;
    }

    @MessageMapping("/send")
    public void send(@Payload SendMessageRequest request, Principal sender) {
        MessageEnvelopeEvent envelope = new MessageEnvelopeEvent(
                UUID.randomUUID().toString(),
                sender.getName(),
                request.recipientId(),
                request.recipientDeviceId(),
                request.ciphertext(),
                Instant.now());
        inboundMessageProducer.publish(envelope);
    }

    @MessageMapping("/ack")
    public void ack(@Payload AckRequest request, Principal recipient) {
        receiptPublisher.publishDelivered(request.messageId(), recipient.getName());
    }
}
