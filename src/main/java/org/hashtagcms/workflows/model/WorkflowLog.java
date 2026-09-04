package org.hashtagcms.workflows.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "workflow_logs")
public class WorkflowLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_alias", nullable = false)
    private String workflowAlias;

    @Column(name = "site_id", nullable = false)
    private Long siteId = 1L;

    @Column(name = "user_id")
    private Long userId;

    /** External (SSO) subject id — set when the caller was resolved via an SSO provider. */
    @Column(name = "external_user_id")
    private String externalUserId;

    /** Alias of the SSO provider that resolved the identity (null for local/anonymous). */
    @Column(name = "sso_provider_alias")
    private String ssoProviderAlias;

    @Column(name = "session_id")
    private String sessionId;

    @Column(columnDefinition = "text")
    @Convert(converter = JsonSupport.MapConverter.class)
    private Map<String, Object> payload;

    @Column(name = "response_directives", columnDefinition = "text")
    @Convert(converter = JsonSupport.ListConverter.class)
    private List<Object> responseDirectives;

    @Column(columnDefinition = "text")
    @Convert(converter = JsonSupport.MapConverter.class)
    private Map<String, Object> negotiation;

    @Column(name = "is_success")
    private boolean success = true;

    @Column(name = "error_message")
    private String errorMessage;

    // PHP maps this as integer('execution_time_ms'); keep it INT to match the shared schema.
    @Column(name = "execution_time_ms")
    private int executionTimeMs;

    @Column(name = "client_platform")
    private String clientPlatform;

    @Column(name = "client_app_version")
    private String clientAppVersion;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    public Long getId() { return id; }
    public String getWorkflowAlias() { return workflowAlias; }
    public void setWorkflowAlias(String v) { this.workflowAlias = v; }
    public Long getSiteId() { return siteId; }
    public void setSiteId(Long v) { this.siteId = v; }
    public Long getUserId() { return userId; }
    public void setUserId(Long v) { this.userId = v; }
    public String getExternalUserId() { return externalUserId; }
    public void setExternalUserId(String v) { this.externalUserId = v; }
    public String getSsoProviderAlias() { return ssoProviderAlias; }
    public void setSsoProviderAlias(String v) { this.ssoProviderAlias = v; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String v) { this.sessionId = v; }
    public Map<String, Object> getPayload() { return payload; }
    public void setPayload(Map<String, Object> v) { this.payload = v; }
    public List<Object> getResponseDirectives() { return responseDirectives; }
    public void setResponseDirectives(List<Object> v) { this.responseDirectives = v; }
    public Map<String, Object> getNegotiation() { return negotiation; }
    public void setNegotiation(Map<String, Object> v) { this.negotiation = v; }
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean v) { this.success = v; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
    public int getExecutionTimeMs() { return executionTimeMs; }
    public void setExecutionTimeMs(long v) { this.executionTimeMs = (int) v; }
    public String getClientPlatform() { return clientPlatform; }
    public void setClientPlatform(String v) { this.clientPlatform = v; }
    public String getClientAppVersion() { return clientAppVersion; }
    public void setClientAppVersion(String v) { this.clientAppVersion = v; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
