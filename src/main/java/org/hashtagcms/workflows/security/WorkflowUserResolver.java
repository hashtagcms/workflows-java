package org.hashtagcms.workflows.security;

import jakarta.servlet.http.HttpServletRequest;

import java.util.Map;

/**
 * Resolves the current user for a workflow execution — the Java analogue of
 * Laravel's {@code auth()->user()}. The package does not authenticate; it reads
 * whoever the host has authenticated. Provide your own bean to integrate with
 * your auth (a gateway, Spring Security, an IdP, JWT verification, …).
 */
public interface WorkflowUserResolver {
    /**
     * @param request the incoming request to read identity from
     * @return the user as a map (must include an {@code id} to count as
     *         authenticated), or {@code null} / empty when there is no user.
     */
    Map<String, Object> resolveUser(HttpServletRequest request);
}
