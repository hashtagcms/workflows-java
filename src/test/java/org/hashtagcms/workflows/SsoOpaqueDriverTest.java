package org.hashtagcms.workflows;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.hashtagcms.workflows.engine.WorkflowResponse;
import org.hashtagcms.workflows.model.JsonSupport;
import org.hashtagcms.workflows.model.Workflow;
import org.hashtagcms.workflows.model.WorkflowLog;
import org.hashtagcms.workflows.model.WorkflowSsoProvider;
import org.hashtagcms.workflows.repository.WorkflowLogRepository;
import org.hashtagcms.workflows.repository.WorkflowRepository;
import org.hashtagcms.workflows.repository.WorkflowSsoProviderRepository;
import org.hashtagcms.workflows.security.SsoIdentityResolver;
import org.hashtagcms.workflows.security.UnauthorizedException;
import org.hashtagcms.workflows.security.WorkflowIdentity;
import org.hashtagcms.workflows.security.sso.OpaqueTokenSsoDriver;
import org.hashtagcms.workflows.service.WorkflowService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Opaque driver + resolver + execution behavior, with the login service's verify
 * endpoint stubbed by an in-process HTTP server. Mirrors the PHP
 * SsoIdentityResolverTest / WorkflowsIdentityResolutionTest coverage.
 */
@SpringBootTest
@Transactional
class SsoOpaqueDriverTest {

    @Autowired OpaqueTokenSsoDriver opaqueDriver;
    @Autowired SsoIdentityResolver resolver;
    @Autowired WorkflowSsoProviderRepository providers;
    @Autowired WorkflowRepository workflows;
    @Autowired WorkflowLogRepository logs;
    @Autowired WorkflowService service;

    private HttpServer server;
    private String base;
    private final AtomicInteger verifyCalls = new AtomicInteger();
    private volatile String lastToken;

    @BeforeEach
    void startStub() throws IOException {
        verifyCalls.set(0);
        lastToken = null;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/verify", ex -> handle(ex, "ext-42"));
        server.createContext("/verify-default", ex -> handle(ex, "ext-default"));
        server.createContext("/verify-pinned", ex -> handle(ex, "ext-pinned"));
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopStub() {
        if (server != null) server.stop(0);
    }

    @SuppressWarnings("unchecked")
    private void handle(HttpExchange ex, String subjectId) throws IOException {
        verifyCalls.incrementAndGet();
        String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        try {
            Map<String, Object> parsed = JsonSupport.MAPPER.readValue(body, Map.class);
            lastToken = String.valueOf(parsed.get("token"));
        } catch (Exception ignored) { lastToken = null; }

        if ("bad".equals(lastToken)) {
            write(ex, 401, "{\"message\":\"invalid\"}");
            return;
        }
        write(ex, 200, "{\"data\":{\"user\":{\"id\":\"" + subjectId
                + "\",\"email\":\"buyer@x.io\",\"roles\":[\"buyer\"]}}}");
    }

    private void write(HttpExchange ex, int status, String json) throws IOException {
        byte[] out = json.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, out.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(out); }
    }

    private WorkflowSsoProvider opaque(String alias, String verifyPath, Map<String, Object> credential) {
        WorkflowSsoProvider p = new WorkflowSsoProvider();
        p.setSiteId(1L);
        p.setName(alias);
        p.setAlias(alias);
        p.setDriver("opaque");
        p.setEnabled(true);
        p.setPublishStatus(true);
        p.setOnFailure("reject");
        p.setCacheTtl(300);
        Map<String, Object> verify = new java.util.LinkedHashMap<>();
        verify.put("url", base + verifyPath);
        verify.put("method", "POST");
        verify.put("headers", Map.of("Accept", "application/json"));
        verify.put("body", Map.of("token", "{{request.bearer_token}}"));
        Map<String, Object> config = new java.util.LinkedHashMap<>();
        config.put("verify", verify);
        config.put("identity", Map.of(
                "user_id", "{{response.body.data.user.id}}",
                "claims", Map.of("email", "{{response.body.data.user.email}}",
                        "roles", "{{response.body.data.user.roles}}")));
        if (credential != null) config.put("credential", credential);
        p.setConfig(config);
        return p;
    }

    private MockHttpServletRequest req(String headerName, String headerValue) {
        MockHttpServletRequest r = new MockHttpServletRequest("POST", "/execute");
        if (headerName != null) r.addHeader(headerName, headerValue);
        return r;
    }

    // --- OpaqueTokenSsoDriver -------------------------------------------------

    @Test
    void mapsVerifiedTokenToExternalIdentityAndForwardsIt() {
        WorkflowIdentity id = opaqueDriver.resolve(opaque("xyz-sso", "/verify", null),
                req("Authorization", "Bearer tok-1"));

        assertThat(id.isAuthenticated()).isTrue();
        assertThat(id.getExternalUserId()).isEqualTo("ext-42");
        assertThat(id.getClaims().get("email")).isEqualTo("buyer@x.io");
        assertThat(lastToken).isEqualTo("tok-1"); // bare token forwarded to verify
    }

    @Test
    void readsTokenFromConfiguredCredentialHeaderAndStripsPrefix() {
        WorkflowIdentity id = opaqueDriver.resolve(
                opaque("hdr-sso", "/verify", Map.of("header", "authToken", "strip_prefix", "Bearer ")),
                req("authToken", "Bearer custom-tok"));

        assertThat(id.getExternalUserId()).isEqualTo("ext-42");
        assertThat(lastToken).isEqualTo("custom-tok");
    }

    @Test
    void configuredHeaderDoesNotFallBackToAuthorization() {
        // Authorization present, but the configured header is absent -> no verify call.
        WorkflowIdentity id = opaqueDriver.resolve(
                opaque("hdr-sso", "/verify", Map.of("header", "authToken")),
                req("Authorization", "Bearer ignored"));

        assertThat(id.isAnonymous()).isTrue();
        assertThat(verifyCalls.get()).isZero();
    }

    @Test
    void rejectsOnNon2xx() {
        WorkflowIdentity id = opaqueDriver.resolve(opaque("xyz-sso", "/verify", null),
                req("Authorization", "Bearer bad"));
        assertThat(id.isFailed()).isTrue();
    }

    @Test
    void anonymousWithoutAToken() {
        WorkflowIdentity id = opaqueDriver.resolve(opaque("xyz-sso", "/verify", null), req(null, null));
        assertThat(id.isAnonymous()).isTrue();
        assertThat(verifyCalls.get()).isZero();
    }

    @Test
    void cachesVerifiedTokenSoVerifyIsCalledOnce() {
        WorkflowSsoProvider p = opaque("cache-sso", "/verify", null);
        opaqueDriver.resolve(p, req("Authorization", "Bearer same"));
        opaqueDriver.resolve(p, req("Authorization", "Bearer same"));
        assertThat(verifyCalls.get()).isEqualTo(1);
    }

    // --- Resolver + execution -------------------------------------------------

    @Test
    void resolverUsesPinnedProviderOverSiteDefault() {
        providers.save(opaque("default-sso", "/verify-default", null));
        providers.save(opaque("pinned-sso", "/verify-pinned", null));

        assertThat(resolver.resolve(req("Authorization", "Bearer t"), 1L, null, Map.of())
                .getExternalUserId()).isEqualTo("ext-default");
        assertThat(resolver.resolve(req("Authorization", "Bearer t"), 1L, "pinned-sso", Map.of())
                .getExternalUserId()).isEqualTo("ext-pinned");
    }

    @Test
    void executeBlocksAuthRequiredWhenAnonymousAndLogsNothingAsSuccess() {
        providers.save(opaque("xyz-sso", "/verify", null));
        Workflow wf = new Workflow();
        wf.setSiteId(1L);
        wf.setName("whoami");
        wf.setAlias("WORKFLOW_SSO_ENFORCE_TEST");
        wf.setAuthRequired(true);
        wf.setConfig(Map.of("target", Map.of("type", "none")));
        workflows.save(wf);

        // No token -> anonymous -> auth_required blocks with 401.
        assertThatThrownBy(() -> service.execute("WORKFLOW_SSO_ENFORCE_TEST", Map.of(), 1L, "android", null,
                List.of(), Map.of(), req(null, null)))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("Authentication required.");

        // ...and the blocked attempt is still written to workflow_logs (as unsuccessful),
        // matching the PHP reference — an audit trail, never recorded as a success.
        List<WorkflowLog> written = logs.findAll().stream()
                .filter(l -> "WORKFLOW_SSO_ENFORCE_TEST".equals(l.getWorkflowAlias()))
                .toList();
        assertThat(written).hasSize(1);
        assertThat(written.get(0).isSuccess()).isFalse();
        assertThat(written.get(0).getErrorMessage()).isEqualTo("Authentication required.");
        assertThat(written.get(0).getUserId()).isNull();
        assertThat(written.get(0).getExternalUserId()).isNull();
    }

    @Test
    void explicitIdentityBypassesResolutionForRequestlessCallers() {
        providers.save(opaque("xyz-sso", "/verify", null)); // SSO module active
        Workflow wf = new Workflow();
        wf.setSiteId(1L);
        wf.setName("job");
        wf.setAlias("WORKFLOW_SSO_JOB_TEST");
        wf.setAuthRequired(true);
        wf.setConfig(Map.of("target", Map.of("type", "none")));
        workflows.save(wf);

        // A queued/server-to-server caller with no HTTP request and no token passes
        // an identity straight in — it wins over resolution and satisfies auth_required.
        WorkflowResponse res = service.execute("WORKFLOW_SSO_JOB_TEST", Map.of(), 1L, "android", null,
                List.of(), Map.of(), null, WorkflowIdentity.from(99));

        assertThat(res.isSuccess()).isTrue();
        assertThat(verifyCalls.get()).isZero(); // no verify call — resolution was bypassed
        WorkflowLog log = logs.findAll().stream()
                .filter(l -> "WORKFLOW_SSO_JOB_TEST".equals(l.getWorkflowAlias()))
                .findFirst().orElseThrow();
        assertThat(log.getUserId()).isEqualTo(99L);
    }

    @Test
    void executeLogsExternalIdentityAndInterpolatesClaims() {
        providers.save(opaque("xyz-sso", "/verify", null));
        Workflow wf = new Workflow();
        wf.setSiteId(1L);
        wf.setName("whoami");
        wf.setAlias("WORKFLOW_SSO_CLAIMS_TEST");
        wf.setAuthRequired(true);
        wf.setConfig(Map.of(
                "target", Map.of("type", "none"),
                "on_success", Map.of("data", Map.of(
                        "email", "{{claims.email}}",
                        "external_user_id", "{{identity.external_user_id}}",
                        "provider", "{{identity.provider}}"))));
        workflows.save(wf);

        WorkflowResponse response = service.execute("WORKFLOW_SSO_CLAIMS_TEST", Map.of(), 1L, "android", null,
                List.of(), Map.of(), req("Authorization", "Bearer good"));

        assertThat(response.toMap().get("success")).isEqualTo(true);
        Map<?, ?> data = (Map<?, ?>) response.toMap().get("data");
        assertThat(data.get("email")).isEqualTo("buyer@x.io");
        assertThat(data.get("external_user_id")).isEqualTo("ext-42");
        assertThat(data.get("provider")).isEqualTo("xyz-sso");

        WorkflowLog last = logs.findAll().stream()
                .filter(l -> "WORKFLOW_SSO_CLAIMS_TEST".equals(l.getWorkflowAlias()))
                .reduce((a, b) -> b).orElseThrow();
        assertThat(last.getExternalUserId()).isEqualTo("ext-42");
        assertThat(last.getSsoProviderAlias()).isEqualTo("xyz-sso");
        assertThat(last.getUserId()).isNull();
    }
}
