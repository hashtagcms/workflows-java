package org.hashtagcms.workflows.config;

import jakarta.annotation.PostConstruct;
import org.hashtagcms.workflows.engine.VariableInterpolator;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Makes {@code {{ env.KEY }}} interpolation resolve through Spring's
 * {@link Environment} — the Java equivalent of Laravel's {@code env()}: it reads
 * `application.yml`/`.properties`, JVM system properties ({@code -Dkey=value}),
 * and OS environment variables (with relaxed binding), in Spring's usual order.
 */
@Component
public class InterpolationConfig {

    private final Environment environment;

    public InterpolationConfig(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    void wireEnvResolver() {
        VariableInterpolator.setEnvResolver(environment::getProperty);
    }
}
