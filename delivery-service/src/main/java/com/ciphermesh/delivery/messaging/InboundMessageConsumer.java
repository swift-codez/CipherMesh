package com.ciphermesh.delivery.messaging;

import com.ciphermesh.delivery.routing.MessageRouter;
import com.ciphermesh.events.MessageEnvelopeEvent;
import com.ciphermesh.events.Topics;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes inbound envelopes from Kafka and hands them to the router. A single
 * consumer group means each message is routed exactly once by whichever
 * instance picks it up; the router then locates the recipient's live session.
 */
@Component
public class InboundMessageConsumer {

    private final MessageRouter messageRouter;

    public InboundMessageConsumer(MessageRouter messageRouter) {
        this.messageRouter = messageRouter;
    }

    @KafkaListener(topics = Topics.MESSAGES_INBOUND, groupId = "${spring.kafka.consumer.group-id}")
    public void onMessage(MessageEnvelopeEvent event) {
        messageRouter.route(event);
    }
}
