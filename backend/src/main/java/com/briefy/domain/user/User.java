package com.briefy.domain.user;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuthProvider provider;

    @Nullable
    @Column(name = "provider_id")
    private String providerId;

    @NonNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @NonNull
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected User() {
    }

    public User(@NonNull String email, @NonNull String name,
                @NonNull AuthProvider provider, @Nullable String providerId) {
        this.email = email;
        this.name = name;
        this.provider = provider;
        this.providerId = providerId;
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
    public AuthProvider getProvider() { return provider; }

    @Nullable
    public String getProviderId() { return providerId; }

    @NonNull
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @NonNull
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void updateName(@NonNull String name) {
        this.name = name;
    }

    public void updatePasswordHash(@Nullable String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void linkOAuth(@NonNull AuthProvider provider, @NonNull String providerId) {
        this.provider = provider;
        this.providerId = providerId;
    }
}
