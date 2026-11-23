package com.fittrack;

import com.fittrack.billing.gateway.PaymentGateway;
import com.fittrack.notification.email.EmailService;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Boots the full application against a real PostgreSQL (Testcontainers), running all Flyway
 * migrations. Skipped automatically when Docker is not available.
 * The payment gateway and email sender are mocked so tests never call Razorpay or SMTP.
 */
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.jwt.secret=aW50ZWdyYXRpb24tdGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2g=",
        "app.cookie.secure=false",
        "app.storage.root=target/test-uploads",
        "app.rate-limit.enabled=false",
        "app.admin.email=admin@test.local",
        "app.admin.password=Admin@123",
        "app.billing.trial-days=7"
})
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    protected static final String ADMIN_EMAIL = "admin@test.local";
    protected static final String ADMIN_PASSWORD = "Admin@123";

    /**
     * Singleton container shared by every integration test class (and the cached Spring context).
     * Started once; Testcontainers' Ryuk removes it when the JVM exits.
     */
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        if (DockerClientFactory.instance().isDockerAvailable()) {
            POSTGRES.start();
        }
    }

    @MockitoBean
    protected PaymentGateway paymentGateway;

    @MockitoBean
    protected EmailService emailService;
}
