package org.hashtagcms.workflows.model;

import jakarta.persistence.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.Instant;
import java.util.Map;

@Entity
@Table(name = "workflow_directives", uniqueConstraints = @UniqueConstraint(columnNames = {"site_id", "type"}))
@SQLRestriction("deleted_at is null") // Laravel soft deletes
public class WorkflowDirective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "site_id", nullable = false)
    private Long siteId = 1L;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(nullable = false)
    private String label;

    private String category;

    @Column(columnDefinition = "text")
    private String description;

    /** platform -> minimum app version (null = universal). */
    @Column(columnDefinition = "text")
    @Convert(converter = JsonSupport.StringMapConverter.class)
    private Map<String, String> platforms;

    // `schema` is reserved — backticks make Hibernate quote it per dialect.
    @Column(name = "`schema`", columnDefinition = "text")
    @Convert(converter = JsonSupport.MapConverter.class)
    private Map<String, Object> schema;

    private String fallback;

    @Column(name = "is_core")
    private boolean core = false;

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
    void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getSiteId() { return siteId; }
    public void setSiteId(Long siteId) { this.siteId = siteId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, String> getPlatforms() { return platforms; }
    public void setPlatforms(Map<String, String> platforms) { this.platforms = platforms; }
    public Map<String, Object> getSchema() { return schema; }
    public void setSchema(Map<String, Object> schema) { this.schema = schema; }
    public String getFallback() { return fallback; }
    public void setFallback(String fallback) { this.fallback = fallback; }
    public boolean isCore() { return core; }
    public void setCore(boolean core) { this.core = core; }
    public boolean isPublishStatus() { return publishStatus; }
    public void setPublishStatus(boolean publishStatus) { this.publishStatus = publishStatus; }
}
