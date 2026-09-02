package org.hashtagcms.workflows;

import org.hashtagcms.workflows.config.SchemaCompatibilityListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;

/**
 * Optional standalone runner — lets you start the engine on its own
 * ({@code ./mvnw spring-boot:run}) for local development, demos, or as a
 * ready-made microservice.
 *
 * <p>It deliberately does <strong>not</strong> {@code @ComponentScan}: the library
 * wires itself through {@link org.hashtagcms.workflows.autoconfigure.WorkflowsAutoConfiguration}
 * (Spring Boot auto-configuration), which is the same path a consuming application
 * uses when it simply adds this artifact as a dependency. One wiring path, whether
 * embedded as a library or run on its own.
 */
@SpringBootConfiguration
@EnableAutoConfiguration
public class WorkflowsApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(WorkflowsApplication.class);
        // Explain a shared-mode schema-validation failure instead of a raw stack trace.
        app.addListeners(new SchemaCompatibilityListener());
        app.run(args);
    }
}
