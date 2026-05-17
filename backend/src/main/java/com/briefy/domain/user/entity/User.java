package com.briefy.domain.user.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
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

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected User() {
    }

    public User(@NonNull String email, @NonNull String name) {
        this.email = email;
        this.name = name;
    }

    public UUID getId() { return id; }

    @NonNull
    public String getEmail() { return email; }

    @Nullable
    public String getPasswordHash() { return passwordHash; }

    @NonNull
    public String getName() { return name; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void updateName(@NonNull String name) {
        this.name = name;
    }

    public void updatePasswordHash(@Nullable String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
