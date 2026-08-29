package org.hashtagcms.workflows.model;

import jakarta.persistence.*;
import org.hibernate.annotations.Immutable;

import java.time.Instant;

/**
 * Read-only view of Laravel Sanctum's `personal_access_tokens` table. The token
 * column stores {@code sha256(secret)}; the wire token is {@code {id}|{secret}}.
 */
@Entity
@Immutable
@Table(name = "personal_access_tokens")
public class PersonalAccessToken {

    @Id
    private Long id;

    @Column(name = "tokenable_type")
    private String tokenableType;

    @Column(name = "tokenable_id")
    private Long tokenableId;

    private String token;

    @Column(name = "expires_at")
    private Instant expiresAt;

    public Long getId() { return id; }
    public String getTokenableType() { return tokenableType; }
    public Long getTokenableId() { return tokenableId; }
    public String getToken() { return token; }
    public Instant getExpiresAt() { return expiresAt; }
}
