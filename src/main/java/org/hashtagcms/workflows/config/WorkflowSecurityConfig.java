package org.hashtagcms.workflows.config;

import org.hashtagcms.workflows.repository.PersonalAccessTokenRepository;
import org.hashtagcms.workflows.repository.UserRepository;
import org.hashtagcms.workflows.security.HeaderWorkflowUserResolver;
import org.hashtagcms.workflows.security.JwtUserResolver;
import org.hashtagcms.workflows.security.SanctumTokenUserResolver;
import org.hashtagcms.workflows.security.WorkflowUserResolver;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WorkflowSecurityConfig {

    /**
     * The user resolver — selected by `hashtagcms.workflows.auth.driver`:
     *   `sanctum` → validate a Laravel Sanctum bearer token against the shared tables;
     *   `jwt`     → verify an external IdP's JWT (OIDC/SSO) via JWKS or a shared secret;
     *   `header`  → trust gateway-forwarded identity headers (default).
     * A host can override auth entirely by declaring its own
     * {@link WorkflowUserResolver} bean (this one backs off).
     */
    @Bean
    @ConditionalOnMissingBean(WorkflowUserResolver.class)
    public WorkflowUserResolver workflowUserResolver(WorkflowProperties properties,
                                                     UserRepository users,
                                                     PersonalAccessTokenRepository tokens) {
        String driver = properties.getAuth().getDriver();
        if ("sanctum".equalsIgnoreCase(driver)) {
            return new SanctumTokenUserResolver(users, tokens);
        }
        if ("jwt".equalsIgnoreCase(driver)) {
            return new JwtUserResolver(properties.getAuth().getJwt(), users);
        }
        return new HeaderWorkflowUserResolver(properties);
    }
}
