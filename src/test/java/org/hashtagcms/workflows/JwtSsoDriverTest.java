package org.hashtagcms.workflows;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.hashtagcms.workflows.model.WorkflowSsoProvider;
import org.hashtagcms.workflows.security.WorkflowIdentity;
import org.hashtagcms.workflows.security.sso.JwtSsoDriver;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The data-driven {@code jwt} SSO driver: a token signed by the "IdP" (HS256
 * shared secret from the provider row) is verified locally and mapped to an
 * identity via the provider's {@code identity} block ({@code {{ token.* }}}).
 */
class JwtSsoDriverTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef01";

    private final JwtSsoDriver driver = new JwtSsoDriver();

    private WorkflowSsoProvider provider() {
        WorkflowSsoProvider p = new WorkflowSsoProvider();
        p.setAlias("idp-sso");
        p.setDriver("jwt");
        p.setConfig(Map.of(
                "secret", SECRET,
                "issuer", "https://issuer.test",
                "identity", Map.of(
                        "user_id", "{{token.sub}}",
                        "claims", Map.of("email", "{{token.email}}"))));
        return p;
    }

    private String token(String issuer, String secret, Instant expiry) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("sso-user-1").issuer(issuer)
                .claim("email", "sam@sso.example")
                .expirationTime(Date.from(expiry)).build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    private MockHttpServletRequest bearer(String token) {
        MockHttpServletRequest r = new MockHttpServletRequest();
        if (token != null) r.addHeader("Authorization", "Bearer " + token);
        return r;
    }

    @Test
    void verifiesAndMapsAValidToken() throws Exception {
        WorkflowIdentity id = driver.resolve(provider(),
                bearer(token("https://issuer.test", SECRET, Instant.now().plusSeconds(300))));

        assertThat(id.isAuthenticated()).isTrue();
        assertThat(id.getExternalUserId()).isEqualTo("sso-user-1");
        assertThat(id.getClaims().get("email")).isEqualTo("sam@sso.example");
        assertThat(id.getProvider()).isEqualTo("idp-sso");
    }

    @Test
    void rejectsAWrongSignature() throws Exception {
        WorkflowIdentity id = driver.resolve(provider(),
                bearer(token("https://issuer.test", "wrongsecretwrongsecretwrongsecret1", Instant.now().plusSeconds(300))));
        assertThat(id.isFailed()).isTrue();
    }

    @Test
    void rejectsAWrongIssuer() throws Exception {
        WorkflowIdentity id = driver.resolve(provider(),
                bearer(token("https://evil.test", SECRET, Instant.now().plusSeconds(300))));
        assertThat(id.isFailed()).isTrue();
    }

    @Test
    void rejectsAnExpiredToken() throws Exception {
        WorkflowIdentity id = driver.resolve(provider(),
                bearer(token("https://issuer.test", SECRET, Instant.now().minusSeconds(3600))));
        assertThat(id.isFailed()).isTrue();
    }

    @Test
    void anonymousWithoutAToken() {
        assertThat(driver.resolve(provider(), bearer(null)).isAnonymous()).isTrue();
    }
}
