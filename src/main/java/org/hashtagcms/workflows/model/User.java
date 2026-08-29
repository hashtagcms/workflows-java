package org.hashtagcms.workflows.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.SQLRestriction;

/**
 * Read-only view of the shared HashtagCMS `users` table (only the columns the
 * workflow service needs). Owned by the PHP app; never written from here.
 */
@Entity
@Immutable
@Table(name = "users")
@SQLRestriction("deleted_at is null")
public class User {

    @Id
    private Long id;

    private String name;

    private String email;

    // Mapped so H2 (standalone) also creates the column the @SQLRestriction uses.
    @Column(name = "deleted_at")
    private java.time.Instant deletedAt;

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getEmail() { return email; }
}
