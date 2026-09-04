package org.hashtagcms.workflows.security;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The normalized result of resolving who is executing a workflow — the Java
 * analogue of the PHP {@code WorkflowIdentity}.
 *
 * <p>A subject is either a <em>local</em> user (a numeric HashtagCMS id, surfaced
 * as {@code workflow_logs.user_id} and {@code {{ user.* }}}) or an <em>external</em>
 * SSO subject (a string id, surfaced as {@code workflow_logs.external_user_id} and
 * {@code {{ claims.* }}} / {@code {{ identity.* }}}). A rejected credential (a
 * bad/expired token under a reject-mode provider) is a distinct {@link #isFailed()}
 * state; absence of any credential is {@link #isAnonymous()}. Never thrown — the
 * caller decides what a failed/anonymous identity means (e.g. a 401).
 */
public final class WorkflowIdentity {

    private final boolean authenticated;
    private final boolean failed;
    private final Long userId;               // local numeric id (workflow_logs.user_id)
    private final String externalUserId;     // external SSO subject id
    private final String provider;           // SSO provider alias (null for local/anonymous)
    private final Map<String, Object> user;  // local user map, for {{ user.* }}
    private final Map<String, Object> claims;
    private final Map<String, Object> raw;

    private WorkflowIdentity(boolean authenticated, boolean failed, Long userId, String externalUserId,
                             String provider, Map<String, Object> user,
                             Map<String, Object> claims, Map<String, Object> raw) {
        this.authenticated = authenticated;
        this.failed = failed;
        this.userId = userId;
        this.externalUserId = externalUserId;
        this.provider = provider;
        this.user = user == null ? Map.of() : user;
        this.claims = claims == null ? Map.of() : claims;
        this.raw = raw == null ? Map.of() : raw;
    }

    /** No credential present — the workflow runs unauthenticated. */
    public static WorkflowIdentity anonymous() {
        return new WorkflowIdentity(false, false, null, null, null, Map.of(), Map.of(), Map.of());
    }

    /** A credential was presented but is invalid/expired (reject-mode failure). */
    public static WorkflowIdentity rejected(String provider) {
        return new WorkflowIdentity(false, true, null, null, provider, Map.of(), Map.of(), Map.of());
    }

    /**
     * A local user, from the host's {@link WorkflowUserResolver} (gateway header /
     * Sanctum / JWT). Authenticated when the map carries an {@code id}; the numeric
     * id (when parseable) becomes {@code workflow_logs.user_id}.
     */
    public static WorkflowIdentity localUser(Map<String, Object> user) {
        if (user == null || user.isEmpty() || user.get("id") == null) {
            return anonymous();
        }
        Long numericId = null;
        try {
            numericId = Long.parseLong(String.valueOf(user.get("id")));
        } catch (NumberFormatException ignored) { /* non-numeric local id -> user_id stays null */ }
        return new WorkflowIdentity(true, false, numericId, null, null, user, Map.of(), Map.of());
    }

    /**
     * Coerce a caller-supplied identity into a {@link WorkflowIdentity} — the Java
     * analogue of PHP's {@code WorkflowIdentity::from()}. Lets a server-to-server or
     * queued caller (with no HTTP request to resolve from) pass identity straight
     * into {@code WorkflowService.execute}:
     * <ul>
     *   <li>a {@link WorkflowIdentity} — used as-is;</li>
     *   <li>a numeric id ({@link Number} or numeric string) — a local user;</li>
     *   <li>a user {@link Map} (e.g. {@code {id,email,name}}) — a local user;</li>
     *   <li>{@code null} — anonymous.</li>
     * </ul>
     */
    @SuppressWarnings("unchecked")
    public static WorkflowIdentity from(Object value) {
        if (value == null) return anonymous();
        if (value instanceof WorkflowIdentity wi) return wi;
        if (value instanceof Map<?, ?> map) return localUser((Map<String, Object>) map);
        if (value instanceof Number n) return localUser(Map.of("id", n.longValue()));
        String s = String.valueOf(value).trim();
        if (s.isEmpty()) return anonymous();
        return localUser(Map.of("id", s));
    }

    /** An external SSO subject: a string id plus mapped claims (and optional raw passthrough). */
    public static WorkflowIdentity external(String id, Map<String, Object> claims,
                                            Map<String, Object> raw, String provider) {
        if (id == null || id.isBlank()) {
            return rejected(provider);
        }
        return new WorkflowIdentity(true, false, null, id, provider, Map.of(), claims, raw);
    }

    public boolean isAuthenticated() { return authenticated && !failed; }
    public boolean isFailed() { return failed; }
    public boolean isAnonymous() { return !authenticated && !failed; }

    public Long getUserId() { return userId; }
    public String getExternalUserId() { return externalUserId; }
    public String getProvider() { return provider; }
    public Map<String, Object> getUser() { return user; }
    public Map<String, Object> getClaims() { return claims; }
    public Map<String, Object> getRaw() { return raw; }

    /** The {@code {{ identity.* }}} interpolation namespace: user_id, external_user_id, provider, raw. */
    public Map<String, Object> identityContext() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("user_id", userId);
        m.put("external_user_id", externalUserId);
        m.put("provider", provider);
        m.put("raw", raw);
        return m;
    }
}
