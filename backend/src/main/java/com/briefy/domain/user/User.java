package com.briefy.domain.user;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @NonNull
    @Column(nullable = false, unique = true)
    private String email;

    @Nullable
    @Column(name = "password_hash")
    private String passwordHash;

    @NonNull
    @Column(nullable = false, length = 100)
    private String name;

    @NonNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> interests = new ArrayList<>();

    @NonNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @NonNull
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected User() {
    }

    public User(@NonNull String email, @NonNull String name) {
        this.email = email;
        this.name = name;
    }

    @PrePersist
    private void prePersist() {
        createdAt = OffsetDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }

    @NonNull
    public String getEmail() { return email; }

    @Nullable
    public String getPasswordHash() { return passwordHash; }

    @NonNull
    public String getName() { return name; }

    @NonNull
    public List<String> getInterests() { return interests; }

    @NonNull
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @NonNull
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void updateName(@NonNull String name) {
        this.name = name;
    }

    public void updateInterests(@NonNull List<String> interests) {
        this.interests = new ArrayList<>(interests);
    }

    public void updatePasswordHash(@Nullable String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
