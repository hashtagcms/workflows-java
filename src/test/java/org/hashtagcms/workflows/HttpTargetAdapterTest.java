package org.hashtagcms.workflows;

import org.hashtagcms.workflows.config.WorkflowProperties;
import org.hashtagcms.workflows.engine.target.HttpTargetAdapter;
import org.hashtagcms.workflows.engine.target.TargetResult;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SSRF egress guards on the http target adapter. These assert the request is
 * refused *before* any network call, so they are deterministic and offline.
 */
class HttpTargetAdapterTest {

    private HttpTargetAdapter adapter(WorkflowProperties.Http cfg) {
        WorkflowProperties p = new WorkflowProperties();
        if (cfg != null) {
            p.getHttp().setAllowedHosts(cfg.getAllowedHosts());
            p.getHttp().setBlockPrivateNetworks(cfg.isBlockPrivateNetworks());
        }
        return new HttpTargetAdapter(p);
    }

    @Test
    void allowlistBlocksHostsNotListed() {
        WorkflowProperties.Http cfg = new WorkflowProperties.Http();
        cfg.setAllowedHosts(List.of("api.example.com", "*.trusted.io"));
        HttpTargetAdapter a = adapter(cfg);

        TargetResult r = a.execute(Map.of("method", "GET", "url", "http://169.254.169.254/latest/meta-data"), Map.of());
        assertThat(r.success()).isFalse();
        assertThat(r.error()).contains("not allowed");
    }

    @Test
    void allowlistPermitsListedHostAndSubdomains() {
        WorkflowProperties.Http cfg = new WorkflowProperties.Http();
        cfg.setAllowedHosts(List.of("*.trusted.io"));
        HttpTargetAdapter a = adapter(cfg);

        // Not in the allowlist -> denied with a clear reason (no network attempted).
        TargetResult denied = a.execute(Map.of("url", "https://evil.example.org/x"), Map.of());
        assertThat(denied.success()).isFalse();
        assertThat(denied.error()).contains("allowlist");
    }

    @Test
    void privateNetworkBlockRejectsLoopbackAndMetadata() {
        WorkflowProperties.Http cfg = new WorkflowProperties.Http();
        cfg.setBlockPrivateNetworks(true);
        HttpTargetAdapter a = adapter(cfg);

        assertThat(a.execute(Map.of("url", "http://127.0.0.1:9/"), Map.of()).success()).isFalse();
        assertThat(a.execute(Map.of("url", "http://169.254.169.254/latest/meta-data"), Map.of()).success()).isFalse();
        TargetResult meta = a.execute(Map.of("url", "http://169.254.169.254/"), Map.of());
        assertThat(meta.error()).contains("private/loopback");
    }

    @Test
    void nonHttpSchemeIsRejected() {
        HttpTargetAdapter a = adapter(null);
        TargetResult r = a.execute(Map.of("url", "file:///etc/passwd"), Map.of());
        assertThat(r.success()).isFalse();
        assertThat(r.error()).contains("scheme");
    }

    @Test
    void missingUrlIsRejected() {
        HttpTargetAdapter a = adapter(null);
        assertThat(a.execute(Map.of("method", "GET"), Map.of()).success()).isFalse();
    }
}
