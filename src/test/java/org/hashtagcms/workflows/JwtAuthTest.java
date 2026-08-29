package org.hashtagcms.workflows;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * The `jwt` SSO driver end-to-end: a token signed by the "IdP" (shared HS256
 * secret) authenticates and flows into {@code {{ user.* }}}; bad/expired/no
 * token is rejected for an auth_required workflow.
 */
@SpringBootTest(properties = {
        "hashtagcms.workflows.auth.driver=jwt",
        "hashtagcms.workflows.auth.jwt.secret=0123456789abcdef0123456789abcdef01",
        "hashtagcms.workflows.auth.jwt.issuer=https://issuer.test",
})
@AutoConfigureMockMvc
class JwtAuthTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef01";
    private static final String EXECUTE = "/api/hashtagcms/public/workflows/v1/execute";

    @Autowired
    MockMvc mvc;

    private String token(String issuer, Instant expiry) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject("sso-user-1")
                .issuer(issuer)
                .claim("email", "sam@sso.example")
                .claim("name", "Sam SSO")
                .expirationTime(Date.from(expiry))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }

    @Test
    void validSsoTokenAuthenticatesAndInterpolates() throws Exception {
        String jwt = token("https://issuer.test", Instant.now().plusSeconds(300));
        mvc.perform(post(EXECUTE).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                        .content("{\"workflow\":\"WORKFLOW_WHOAMI\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Hello sam@sso.example"))
                .andExpect(jsonPath("$.data.userId").value("sso-user-1"));
    }

    @Test
    void noTokenRejected() throws Exception {
        mvc.perform(post(EXECUTE).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workflow\":\"WORKFLOW_WHOAMI\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void wrongIssuerRejected() throws Exception {
        String jwt = token("https://evil.test", Instant.now().plusSeconds(300));
        mvc.perform(post(EXECUTE).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                        .content("{\"workflow\":\"WORKFLOW_WHOAMI\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredTokenRejected() throws Exception {
        String jwt = token("https://issuer.test", Instant.now().minusSeconds(600));
        mvc.perform(post(EXECUTE).contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + jwt)
                        .content("{\"workflow\":\"WORKFLOW_WHOAMI\"}"))
                .andExpect(status().isUnauthorized());
    }
}
