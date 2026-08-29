package org.hashtagcms.workflows.security;

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
import org.hashtagcms.workflows.config.WorkflowProperties;
import org.hashtagcms.workflows.model.User;
import org.hashtagcms.workflows.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Resolves the user from an external IdP's JWT (OIDC / SSO) — Auth0, Keycloak,
 * Cognito, Okta, Azure AD, Google, or any provider that issues signed JWTs.
 * Reads {@code Authorization: Bearer <jwt>}, verifies the signature against the
 * configured JWKS endpoint (asymmetric) or shared secret (symmetric), validates
 * {@code iss}/{@code aud}/{@code exp}, then maps claims to {@code {id,email,name}}.
 *
 * <p>The package still does not <em>issue</em> tokens; it trusts your IdP. Signature
 * failures, wrong issuer/audience, or expiry all resolve to {@code null} (no user),
 * which surfaces as 401 for {@code auth_required} workflows.
 */
public class JwtUserResolver implements WorkflowUserResolver {

    private static final Logger log = LoggerFactory.getLogger(JwtUserResolver.class);

    private final WorkflowProperties.Jwt config;
    private final UserRepository users;         // only used when link-by-email is on
    private final ConfigurableJWTProcessor<SecurityContext> processor; // null when misconfigured

    public JwtUserResolver(WorkflowProperties.Jwt config, UserRepository users) {
        this.config = config;
        this.users = users;
        this.processor = buildProcessor(config);
    }

    @Override
    public Map<String, Object> resolveUser(HttpServletRequest request) {
        if (request == null || processor == null) return null;

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        String token = header.substring(7).trim();
        if (token.chars().filter(c -> c == '.').count() != 2) return null; // not a JWS

        JWTClaimsSet claims;
        try {
            claims = processor.process(token, null);
        } catch (Exception e) {
            // bad signature / expired / wrong issuer / malformed — treat as unauthenticated
            return null;
        }
        if (!audienceOk(claims)) return null;

        String id = claim(claims, config.getIdClaim());
        String email = claim(claims, config.getEmailClaim());
        String name = claim(claims, config.getNameClaim());
        if (id == null && email == null) return null; // nothing to identify the user by

        // Optionally adopt the local account's canonical id/email/name.
        if (config.isLinkByEmail() && email != null && users != null) {
            User local = users.findByEmailIgnoreCase(email).orElse(null);
            if (local != null) {
                id = String.valueOf(local.getId());
                if (local.getEmail() != null) email = local.getEmail();
                if (local.getName() != null) name = local.getName();
            }
        }

        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", id != null ? id : email);   // id must be present to count as authenticated
        if (email != null) user.put("email", email);
        if (name != null) user.put("name", name);
        return user;
    }

    private boolean audienceOk(JWTClaimsSet claims) {
        String expected = config.getAudience();
        if (expected == null || expected.isBlank()) return true;
        List<String> aud = claims.getAudience();
        return aud != null && aud.contains(expected);
    }

    private static String claim(JWTClaimsSet claims, String name) {
        if (name == null || name.isBlank()) return null;
        Object v = claims.getClaim(name);
        return v == null ? null : String.valueOf(v);
    }

    /**
     * Builds a verifier from exactly one key source. Returns {@code null} (fail closed)
     * when neither a JWKS URI nor a secret is configured.
     */
    private static ConfigurableJWTProcessor<SecurityContext> buildProcessor(WorkflowProperties.Jwt cfg) {
        try {
            boolean hasJwks = cfg.getJwksUri() != null && !cfg.getJwksUri().isBlank();
            boolean hasSecret = cfg.getSecret() != null && !cfg.getSecret().isBlank();
            if (hasJwks == hasSecret) { // neither, or both
                log.warn("auth.driver=jwt requires exactly one of jwt.jwks-uri or jwt.secret; JWT auth is disabled.");
                return null;
            }

            JWSKeySelector<SecurityContext> keySelector;
            if (hasJwks) {
                JWSAlgorithm alg = cfg.getAlgorithm() != null ? JWSAlgorithm.parse(cfg.getAlgorithm()) : JWSAlgorithm.RS256;
                JWKSource<SecurityContext> keys = new RemoteJWKSet<>(new URL(cfg.getJwksUri()));
                keySelector = new JWSVerificationKeySelector<>(alg, keys);
            } else {
                JWSAlgorithm alg = cfg.getAlgorithm() != null ? JWSAlgorithm.parse(cfg.getAlgorithm()) : JWSAlgorithm.HS256;
                JWKSource<SecurityContext> keys = new ImmutableSecret<>(cfg.getSecret().getBytes(StandardCharsets.UTF_8));
                keySelector = new JWSVerificationKeySelector<>(alg, keys);
            }

            DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
            processor.setJWSKeySelector(keySelector);

            JWTClaimsSet exactMatch = cfg.getIssuer() != null && !cfg.getIssuer().isBlank()
                    ? new JWTClaimsSet.Builder().issuer(cfg.getIssuer()).build()
                    : null;
            DefaultJWTClaimsVerifier<SecurityContext> verifier =
                    new DefaultJWTClaimsVerifier<>(exactMatch, Set.of("exp"));
            verifier.setMaxClockSkew((int) cfg.getClockSkewSeconds());
            processor.setJWTClaimsSetVerifier(verifier);
            return processor;
        } catch (Exception e) {
            log.warn("Failed to initialise the JWT verifier; JWT auth is disabled: {}", e.getMessage());
            return null;
        }
    }
}
