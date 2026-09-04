package org.hashtagcms.workflows.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.Map;

/**
 * A data-driven SSO / external-login provider (the {@code workflow_sso_providers}
 * table owned by the PHP package). A provider row tells the SSO resolver how to
 * verify a client credential and map it to a workflow identity: the verification
 * detail lives in {@link #config} (a {@code verify} request formatter + an
 * {@code identity} response mapper for {@code opaque}, or {@code jwks_url}/
 * {@code issuer}/{@code audience}/{@code identity} for {@code jwt}).
 *
 * <p>Per-site with a master-site fallback, {@code alias} unique per site. No FK to
 * a users table — a provider resolves an external subject that may have no local
 * row (see {@code workflow_logs.external_user_id}).
 */
@Entity
@Table(name = "workflow_sso_providers",
        uniqueConstraints = @UniqueConstraint(name = "workflow_sso_providers_site_alias_unique",
                columnNames = {"site_id", "alias"}))
@SQLRestriction("deleted_at is null") // Laravel soft deletes
public class WorkflowSsoProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId = 1L;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String alias;

    @Column(columnDefinition = "text")
    private String description;

    /** {@code opaque} (introspection call) or {@code jwt} (local JWKS verify). */
    @Column(nullable = false)
    private String driver = "opaque";

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(columnDefinition = "text")
    @Convert(converter = JsonSupport.MapConverter.class)
    private Map<String, Object> config;

    /** {@code reject} (401 on invalid token) or {@code anonymous} (run unauthenticated). */
    @Column(name = "on_failure", nullable = false)
    private String onFailure = "reject";

    /** Seconds to cache a verified token (opaque driver). */
    @Column(name = "cache_ttl", nullable = false)
    private int cacheTtl = 300;

    @Column(name = "publish_status", nullable = false)
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
    public String getDriver() { return driver; }
    public void setDriver(String driver) { this.driver = driver; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Map<String, Object> getConfig() { return config; }
    public void setConfig(Map<String, Object> config) { this.config = config; }
    public String getOnFailure() { return onFailure; }
    public void setOnFailure(String onFailure) { this.onFailure = onFailure; }
    public int getCacheTtl() { return cacheTtl; }
    public void setCacheTtl(int cacheTtl) { this.cacheTtl = cacheTtl; }
    public boolean isPublishStatus() { return publishStatus; }
    public void setPublishStatus(boolean publishStatus) { this.publishStatus = publishStatus; }
    public Long getInsertBy() { return insertBy; }
    public void setInsertBy(Long insertBy) { this.insertBy = insertBy; }
    public Long getUpdateBy() { return updateBy; }
    public void setUpdateBy(Long updateBy) { this.updateBy = updateBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
