package org.hashtagcms.workflows.security.sso;

import jakarta.servlet.http.HttpServletRequest;
import org.hashtagcms.workflows.engine.VariableInterpolator;
import org.hashtagcms.workflows.engine.target.HttpTargetAdapter;
import org.hashtagcms.workflows.engine.target.TargetResult;
import org.hashtagcms.workflows.model.WorkflowSsoProvider;
import org.hashtagcms.workflows.security.WorkflowIdentity;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Opaque-token SSO driver: introspects the token by CALLING the login service's
 * verify endpoint (reusing the {@code http} target adapter as the request
 * formatter), then maps the response through the provider's {@code identity}
 * block. Verified tokens are cached per token-hash for {@code cache_ttl} seconds;
 * failures are never cached.
 */
@Component
public class OpaqueTokenSsoDriver extends AbstractSsoDriver {

    private final HttpTargetAdapter http;
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public OpaqueTokenSsoDriver(HttpTargetAdapter http) {
        this.http = http;
    }

    @Override
    public String type() {
        return "opaque";
    }

    @Override
    @SuppressWarnings("unchecked")
    public WorkflowIdentity resolve(WorkflowSsoProvider provider, HttpServletRequest request) {
        Map<String, Object> config = provider.getConfig() == null ? Map.of() : provider.getConfig();
        String token = credential(request, config);
        if (token == null) {
            return WorkflowIdentity.anonymous(); // no credential -> nothing to verify
        }

        String cacheKey = provider.getAlias() + ":" + sha256(token);
        CacheEntry cached = cache.get(cacheKey);
        if (cached != null && cached.expiresAt > System.currentTimeMillis()) {
            return cached.identity;
        }
        cache.remove(cacheKey);

        Object verifyRaw = config.get("verify");
        if (!(verifyRaw instanceof Map) || ((Map<String, Object>) verifyRaw).get("url") == null) {
            return WorkflowIdentity.rejected(provider.getAlias());
        }

        Map<String, Object> reqCtx = requestContext(request, token);
        Map<String, Object> verify = (Map<String, Object>) VariableInterpolator.interpolate(verifyRaw, reqCtx);
        TargetResult result = http.execute(verify, reqCtx);

        if (!result.success() || result.status() >= 400) {
            return WorkflowIdentity.rejected(provider.getAlias());
        }

        Map<String, Object> ctx = new LinkedHashMap<>(reqCtx);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", result.success());
        response.put("status", result.status());
        response.put("body", result.body() == null ? "" : result.body());
        response.put("headers", result.headers());
        ctx.put("response", response);

        Object identityBlock = config.get("identity");
        WorkflowIdentity identity = identityBlock instanceof Map
                ? buildIdentity((Map<String, Object>) identityBlock, ctx, provider.getAlias())
                : WorkflowIdentity.rejected(provider.getAlias());

        if (identity.isAuthenticated() && provider.getCacheTtl() > 0) {
            cache.put(cacheKey, new CacheEntry(identity,
                    System.currentTimeMillis() + provider.getCacheTtl() * 1000L));
        }
        return identity;
    }

    private static String sha256(String s) {
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(d.length * 2);
            for (byte b : d) sb.append(Character.forDigit((b >> 4) & 0xF, 16)).append(Character.forDigit(b & 0xF, 16));
            return sb.toString();
        } catch (Exception e) {
            return Integer.toHexString(s.hashCode());
        }
    }

    private record CacheEntry(WorkflowIdentity identity, long expiresAt) {}
}
