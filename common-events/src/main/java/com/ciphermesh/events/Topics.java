package com.ciphermesh.events;

/**
 * Canonical Kafka topic names shared across CipherMesh services. Keeping these
 * in one place prevents producers and consumers from drifting apart.
 */
public final class Topics {

    /** Published by identity-service when a new user/device registers. */
    public static final String USER_REGISTERED = "identity.user-registered";

    /** Published by identity-service when a device's one-time pre-key pool runs low. */
    public static final String PREKEYS_LOW = "identity.prekeys-low";

    /** Encrypted message envelopes ingested for routing and store-and-forward. */
    public static final String MESSAGES_INBOUND = "messages.inbound";

    /** Delivery and read receipts emitted by delivery-service. */
    public static final String MESSAGES_RECEIPTS = "messages.receipts";

    private Topics() {
    }
}
