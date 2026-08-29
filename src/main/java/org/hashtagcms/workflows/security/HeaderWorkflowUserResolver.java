package org.hashtagcms.workflows.security;

import jakarta.servlet.http.HttpServletRequest;
import org.hashtagcms.workflows.config.WorkflowProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Default resolver for a trusted-gateway deployment: the gateway authenticates
 * the request (session, JWT, …) and forwards the identity as headers
 * (defaults {@code X-User-Id}, {@code X-User-Email}, {@code X-User-Name}). The
 * user is considered authenticated only when the id header is present.
 *
 * Override the {@code WorkflowUserResolver} bean to plug in Spring Security, JWT
 * verification, or any other scheme.
 */
public class HeaderWorkflowUserResolver implements WorkflowUserResolver {

    private final WorkflowProperties.Auth config;

    public HeaderWorkflowUserResolver(WorkflowProperties properties) {
        this.config = properties.getAuth();
    }

    @Override
    public Map<String, Object> resolveUser(HttpServletRequest request) {
        if (request == null) return null;
        String id = request.getHeader(config.getIdHeader());
        if (id == null || id.isBlank()) return null;

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", id);
        putIfPresent(user, "email", request.getHeader(config.getEmailHeader()));
        putIfPresent(user, "name", request.getHeader(config.getNameHeader()));
        return user;
    }

    private void putIfPresent(Map<String, Object> user, String key, String value) {
        if (value != null && !value.isBlank()) user.put(key, value);
    }
}
