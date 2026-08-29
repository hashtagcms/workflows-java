package org.hashtagcms.workflows.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/** Body for POST /execute. */
public class ExecuteRequest {

    private String workflow;
    private Map<String, Object> payload;

    @JsonProperty("site_id")
    private Long siteId;

    private String platform;
    private Client client;
    private List<String> capabilities;

    public String getWorkflow() { return workflow; }
    public void setWorkflow(String workflow) { this.workflow = workflow; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> payload) { this.payload = payload; }
    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public Client getClient() { return client; }
    public void setClient(Client client) { this.client = client; }
    public List<String> getCapabilities() { return capabilities; }
    public void setCapabilities(List<String> capabilities) { this.capabilities = capabilities; }

    /** The richer client-identity block: { platform, app_version }. */
    public static class Client {
        private String platform;
        @JsonProperty("app_version")
        private String appVersion;
        public String getPlatform() { return platform; }
        public void setPlatform(String platform) { this.platform = platform; }
        public String getAppVersion() { return appVersion; }
        public void setAppVersion(String appVersion) { this.appVersion = appVersion; }
    }
}
