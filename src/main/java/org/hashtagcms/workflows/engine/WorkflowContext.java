package org.hashtagcms.workflows.engine;

import org.hashtagcms.workflows.model.Workflow;

import java.util.List;
import java.util.Map;

/** Immutable execution context handed to the engine / a handler. */
public class WorkflowContext {

    private final Workflow workflow;
    private final Map<String, Object> payload;
    private final long siteId;
    private final String platform;
    private final String appVersion;
    private final List<String> capabilities;
    private final Map<String, Object> user;
    private final Map<String, Object> claims;   // {{ claims.* }} (SSO identity attributes)
    private final Map<String, Object> identity;  // {{ identity.* }}: user_id, external_user_id, provider, raw

    public WorkflowContext(Workflow workflow, Map<String, Object> payload, long siteId,
                           String platform, String appVersion, List<String> capabilities,
                           Map<String, Object> user) {
        this(workflow, payload, siteId, platform, appVersion, capabilities, user, Map.of(), Map.of());
    }

    public WorkflowContext(Workflow workflow, Map<String, Object> payload, long siteId,
                           String platform, String appVersion, List<String> capabilities,
                           Map<String, Object> user, Map<String, Object> claims, Map<String, Object> identity) {
        this.workflow = workflow;
        this.payload = payload;
        this.siteId = siteId;
        this.platform = platform;
        this.appVersion = appVersion;
        this.capabilities = capabilities;
        this.user = user;
        this.claims = claims == null ? Map.of() : claims;
        this.identity = identity == null ? Map.of() : identity;
    }

    public Workflow getWorkflow() { return workflow; }
    public Map<String, Object> getPayload() { return payload; }
    public long getSiteId() { return siteId; }
    public String getPlatform() { return platform; }
    public String getAppVersion() { return appVersion; }
    public List<String> getCapabilities() { return capabilities; }
    public Map<String, Object> getUser() { return user; }
    public Map<String, Object> getClaims() { return claims; }
    public Map<String, Object> getIdentity() { return identity; }

    public Object get(String key, Object def) {
        Object v = payload == null ? null : payload.get(key);
        return v == null ? def : v;
    }
}
