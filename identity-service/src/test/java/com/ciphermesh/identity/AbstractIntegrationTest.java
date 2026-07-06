package com.ciphermesh.identity;

import com.ciphermesh.identity.messaging.IdentityEventPublisher;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base for full-context integration tests using the singleton-container pattern:
 * a single PostgreSQL instance is started once in a static initializer and lives
 * for the whole JVM (reaped by Ryuk at exit). Because Spring caches and reuses
 * the application context across integration-test classes with identical
 * configuration, the container must NOT be tied to the per-class lifecycle of
 * {@code @Testcontainers}/{@code @Container} — otherwise the second test class
 * would inherit a cached datasource pointing at a container that was already
 * stopped after the first class.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    /**
     * Integration tests exercise persistence and HTTP, not the message broker,
     * so the Kafka publisher is mocked. This keeps the identity ITs independent
     * of a running broker and avoids the producer's metadata-fetch block.
     */
    @MockitoBean
    protected IdentityEventPublisher identityEventPublisher;
}
