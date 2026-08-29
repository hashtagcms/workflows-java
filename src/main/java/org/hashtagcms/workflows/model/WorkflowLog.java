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
    public void setWorkflowAlias(String v) { this.workflowAlias = v; }
    public void setSiteId(Long v) { this.siteId = v; }
    public void setUserId(Long v) { this.userId = v; }
    public void setSessionId(String v) { this.sessionId = v; }
    public void setPayload(Map<String, Object> v) { this.payload = v; }
    public void setResponseDirectives(List<Object> v) { this.responseDirectives = v; }
    public void setNegotiation(Map<String, Object> v) { this.negotiation = v; }
    public void setSuccess(boolean v) { this.success = v; }
    public void setErrorMessage(String v) { this.errorMessage = v; }
    public void setExecutionTimeMs(long v) { this.executionTimeMs = (int) v; }
    public void setClientPlatform(String v) { this.clientPlatform = v; }
    public void setClientAppVersion(String v) { this.clientAppVersion = v; }
}
