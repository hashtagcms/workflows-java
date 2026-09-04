package org.hashtagcms.workflows.security.sso;

import jakarta.servlet.http.HttpServletRequest;
import org.hashtagcms.workflows.engine.VariableInterpolator;
import org.hashtagcms.workflows.security.WorkflowIdentity;

import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Shared plumbing for SSO drivers: pulling the credential off the request,
 * exposing it to config interpolation, and mapping a verified payload through a
 * provider's {@code identity} block into a {@link WorkflowIdentity}.
 */
public abstract class AbstractSsoDriver implements SsoDriver {

    /**
     * The credential presented by the client, or null when absent.
     *
     * <p>By default this is the standard {@code Authorization: Bearer <token>}
     * header. Many real APIs carry the token in a different header (e.g.
     * {@code authToken}), sometimes with a prefix — a provider can point at it via
     * a {@code credential} block in its config:
     * <pre>"credential": { "header": "authToken", "strip_prefix": "Bearer " }</pre>
     * When {@code credential.header} is set, that header is read (and the optional
     * {@code strip_prefix} removed); otherwise it falls back to Authorization
     * bearer. A configured header is authoritative — no fallback.
     */
    protected String credential(HttpServletRequest request, Map<String, Object> config) {
        if (request == null) return null;

        Object cred = config == null ? null : config.get("credential");
        if (cred instanceof Map<?, ?> credMap && credMap.get("header") != null
                && !String.valueOf(credMap.get("header")).isBlank()) {
            String value = request.getHeader(String.valueOf(credMap.get("header")));
            if (value == null) return null;

            Object prefix = credMap.get("strip_prefix");
            if (prefix instanceof String p && !p.isEmpty() && value.startsWith(p)) {
                value = value.substring(p.length());
            }
            value = value.trim();
            return value.isEmpty() ? null : value;
        }

        return bearerToken(request);
    }

    /** Standard {@code Authorization: Bearer <token>} extraction. */
    protected String bearerToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null) return null;
        String prefix = "Bearer ";
        if (header.regionMatches(true, 0, prefix, 0, prefix.length())) {
            String token = header.substring(prefix.length()).trim();
            return token.isEmpty() ? null : token;
        }
        return null;
    }

    /**
     * Interpolation source for the verify request: {@code {{request.bearer_token}}},
     * {@code {{request.headers.*}}} (lower-cased header names, matching PHP),
     * {@code {{request.query.*}}}.
     */
    protected Map<String, Object> requestContext(HttpServletRequest request, String token) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("bearer_token", token);
        req.put("token", token);
        req.put("headers", headerMap(request));
        req.put("query", queryMap(request));
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("request", req);
        return ctx;
    }

    private Map<String, Object> headerMap(HttpServletRequest request) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (request == null) return out;
        Enumeration<String> names = request.getHeaderNames();
        if (names == null) return out;
        for (String name : Collections.list(names)) {
            out.put(name.toLowerCase(), request.getHeader(name));
        }
        return out;
    }

    private Map<String, Object> queryMap(HttpServletRequest request) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (request == null || request.getParameterMap() == null) return out;
        request.getParameterMap().forEach((k, v) -> out.put(k, v != null && v.length > 0 ? v[0] : null));
        return out;
    }

    /**
     * Map a verified payload (available to interpolation as {@code context}) through
     * the provider's {@code identity} block into a normalized identity. External
     * subjects are stringified so they route to {@code external_user_id}.
     */
    @SuppressWarnings("unchecked")
    protected WorkflowIdentity buildIdentity(Map<String, Object> identityBlock,
                                             Map<String, Object> context, String provider) {
        Object rawId = VariableInterpolator.interpolate(identityBlock.get("user_id"), context);
        if (rawId == null || String.valueOf(rawId).isEmpty()) {
            // Verified, but the configured user_id path resolved to nothing.
            return WorkflowIdentity.rejected(provider);
        }

        Object claims = identityBlock.containsKey("claims")
                ? VariableInterpolator.interpolate(identityBlock.get("claims"), context) : Map.of();
        // Opt-in raw passthrough -> {{ identity.raw.* }}.
        Object raw = identityBlock.containsKey("raw")
                ? VariableInterpolator.interpolate(identityBlock.get("raw"), context) : Map.of();

        return WorkflowIdentity.external(
                String.valueOf(rawId),
                claims instanceof Map ? (Map<String, Object>) claims : Map.of(),
                raw instanceof Map ? (Map<String, Object>) raw : Map.of(),
                provider);
    }
}
