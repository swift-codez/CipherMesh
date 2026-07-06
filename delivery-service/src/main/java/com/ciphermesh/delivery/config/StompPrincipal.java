package com.ciphermesh.delivery.config;

import java.security.Principal;

/**
 * Minimal {@link Principal} whose name is the authenticated user id. Used as the
 * target for Spring's user destinations (e.g. {@code /user/queue/messages}).
 */
public record StompPrincipal(String name) implements Principal {

    @Override
    public String getName() {
        return name;
    }
}
