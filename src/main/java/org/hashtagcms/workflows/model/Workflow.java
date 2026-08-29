package org.hashtagcms.workflows.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "workflows", uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "alias"}))
@SQLRestriction("deleted_at is null") // Laravel soft deletes
public class Workflow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId = 1L;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 100)
    private String alias;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "auth_required")
    private boolean authRequired = false;

    /** Fully-qualified class name of a WorkflowHandler for code-based workflows. */
    private String handler;

    @Column(columnDefinition = "text")
    @Convert(converter = JsonSupport.MapConverter.class)
    private Map<String, Object> config;

    @Column(name = "publish_status")
    private boolean publishStatus = true;

    @Column(name = "insert_by")
    private Long insertBy;

    @Column(name = "update_by")
    private Long updateBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    // --- getters / setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getAlias() { return alias; }
    public void setAlias(String alias) { this.alias = alias; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public boolean isAuthRequired() { return authRequired; }
    public void setAuthRequired(boolean authRequired) { this.authRequired = authRequired; }
    public String getHandler() { return handler; }
    public void setHandler(String handler) { this.handler = handler; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
    public boolean isPublishStatus() { return publishStatus; }
    public void setPublishStatus(boolean publishStatus) { this.publishStatus = publishStatus; }
    public Long getInsertBy() { return insertBy; }
    public void setInsertBy(Long insertBy) { this.insertBy = insertBy; }
    public Long getUpdateBy() { return updateBy; }
    public void setUpdateBy(Long updateBy) { this.updateBy = updateBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
