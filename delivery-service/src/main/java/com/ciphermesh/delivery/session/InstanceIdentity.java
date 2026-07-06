package com.ciphermesh.delivery.session;

import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * A stable identifier for this delivery-service instance, generated at startup.
 * The session registry records which instance a device is connected to so that
 * messages can be routed to the node holding the live WebSocket.
 */
@Component
public class InstanceIdentity {

    private final String id = UUID.randomUUID().toString();

    public String id() {
        return id;
    }
}
