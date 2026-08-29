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

    public WorkflowContext(Workflow workflow, Map<String, Object> payload, long siteId,
                           String platform, String appVersion, List<String> capabilities,
                           Map<String, Object> user) {
        this.workflow = workflow;
        this.payload = payload;
        this.siteId = siteId;
        this.platform = platform;
        this.appVersion = appVersion;
        this.capabilities = capabilities;
        this.user = user;
    }

    public Workflow getWorkflow() { return workflow; }
    public Map<String, Object> getPayload() { return payload; }
    public long getSiteId() { return siteId; }
    public String getPlatform() { return platform; }
    public String getAppVersion() { return appVersion; }
    public List<String> getCapabilities() { return capabilities; }
    public Map<String, Object> getUser() { return user; }

    public Object get(String key, Object def) {
        Object v = payload == null ? null : payload.get(key);
        return v == null ? def : v;
    }
}
