package org.hashtagcms.workflows.security.sso;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import jakarta.servlet.http.HttpServletRequest;
import org.hashtagcms.workflows.model.WorkflowSsoProvider;
import org.hashtagcms.workflows.security.WorkflowIdentity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JWT SSO driver: verifies the token signature locally against the provider's JWKS
 * ({@code config.jwks_url}) or shared {@code config.secret}, enforces
 * {@code issuer}/{@code audience}, then maps the token claims to an identity via
 * the provider's {@code identity} block ({@code {{ token.* }}}). No per-request
 * introspection call. Verifiers are cached per provider config.
 */
@Component
public class JwtSsoDriver extends AbstractSsoDriver {

    private static final Logger log = LoggerFactory.getLogger(JwtSsoDriver.class);

    private final Map<String, ConfigurableJWTProcessor<SecurityContext>> processors = new ConcurrentHashMap<>();

    @Override
    public String type() {
        return "jwt";
    }

    @Override
    @SuppressWarnings("unchecked")
    public WorkflowIdentity resolve(WorkflowSsoProvider provider, HttpServletRequest request) {
        Map<String, Object> config = provider.getConfig() == null ? Map.of() : provider.getConfig();
        String token = credential(request, config);
        if (token == null || token.chars().filter(c -> c == '.').count() != 2) {
            return WorkflowIdentity.anonymous(); // no JWS credential
        }

        ConfigurableJWTProcessor<SecurityContext> processor = processorFor(provider, config);
        if (processor == null) {
            throw new IllegalStateException("SSO provider '" + provider.getAlias()
                    + "' (jwt) is misconfigured: set exactly one of config.jwks_url or config.secret.");
        }

        JWTClaimsSet claims;
        try {
            claims = processor.process(token, null);
        } catch (Exception e) {
            return WorkflowIdentity.rejected(provider.getAlias()); // bad signature / expired / wrong issuer
        }
        String audience = str(config.get("audience"));
        if (audience != null && !audience.isBlank()
                && (claims.getAudience() == null || !claims.getAudience().contains(audience))) {
            return WorkflowIdentity.rejected(provider.getAlias());
        }

        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("token", claims.getClaims());

        Object identityBlock = config.get("identity");
        return identityBlock instanceof Map
                ? buildIdentity((Map<String, Object>) identityBlock, ctx, provider.getAlias())
                : WorkflowIdentity.rejected(provider.getAlias());
    }

    private ConfigurableJWTProcessor<SecurityContext> processorFor(WorkflowSsoProvider provider, Map<String, Object> config) {
        String jwks = str(config.get("jwks_url"));
        String secret = str(config.get("secret"));
        String issuer = str(config.get("issuer"));
        String algorithm = str(config.get("algorithm"));
        long skew = config.get("clock_skew_seconds") instanceof Number n ? n.longValue() : 60L;
        String signature = provider.getAlias() + "|" + jwks + "|" + secret + "|" + issuer + "|" + algorithm + "|" + skew;

        return processors.computeIfAbsent(signature, k -> build(jwks, secret, issuer, algorithm, skew));
    }

    private ConfigurableJWTProcessor<SecurityContext> build(String jwks, String secret, String issuer,
                                                            String algorithm, long skew) {
        try {
            boolean hasJwks = jwks != null && !jwks.isBlank();
            boolean hasSecret = secret != null && !secret.isBlank();
            if (hasJwks == hasSecret) return null; // neither, or both

            JWSKeySelector<SecurityContext> keySelector;
            if (hasJwks) {
                JWSAlgorithm alg = algorithm != null && !algorithm.isBlank() ? JWSAlgorithm.parse(algorithm) : JWSAlgorithm.RS256;
                JWKSource<SecurityContext> keys = new RemoteJWKSet<>(new URL(jwks));
                keySelector = new JWSVerificationKeySelector<>(alg, keys);
            } else {
                JWSAlgorithm alg = algorithm != null && !algorithm.isBlank() ? JWSAlgorithm.parse(algorithm) : JWSAlgorithm.HS256;
                JWKSource<SecurityContext> keys = new ImmutableSecret<>(secret.getBytes(StandardCharsets.UTF_8));
                keySelector = new JWSVerificationKeySelector<>(alg, keys);
            }

            DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            processor.setJWSKeySelector(keySelector);
            JWTClaimsSet exactMatch = issuer != null && !issuer.isBlank()
                    ? new JWTClaimsSet.Builder().issuer(issuer).build() : null;
            DefaultJWTClaimsVerifier<SecurityContext> verifier =
                    new DefaultJWTClaimsVerifier<>(exactMatch, Set.of("exp"));
            verifier.setMaxClockSkew((int) skew);
            processor.setJWTClaimsSetVerifier(verifier);
            return processor;
        } catch (Exception e) {
            log.warn("Failed to build JWT verifier for SSO provider: {}", e.getMessage());
            return null;
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
