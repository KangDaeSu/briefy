package com.briefy.domain.news;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "news_articles")
public class NewsArticle {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @NonNull
    @Column(nullable = false, columnDefinition = "text")
    private String url;

    // SHA-256 hex of url — 중복 기사 필터링 기준
    @NonNull
    @Column(name = "url_hash", nullable = false, length = 64, unique = true)
    private String urlHash;

    @NonNull
    @Column(nullable = false, columnDefinition = "text")
    private String title;

    @Nullable
    private String source;

    @Nullable
    private String author;

    @Nullable
    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Nullable
    @Column(columnDefinition = "text")
    private String content;

    @Column(name = "embedded", nullable = false)
    private boolean embedded = false;

    @NonNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected NewsArticle() {
    }

    public NewsArticle(@NonNull String url, @NonNull String urlHash, @NonNull String title) {
        this.url = url;
        this.urlHash = urlHash;
        this.title = title;
    }

    @PrePersist
    private void prePersist() {
        createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }

    @NonNull
    public String getUrl() { return url; }

    @NonNull
    public String getUrlHash() { return urlHash; }

    @NonNull
    public String getTitle() { return title; }

    @Nullable
    public String getSource() { return source; }

    @Nullable
    public String getAuthor() { return author; }

    @Nullable
    public OffsetDateTime getPublishedAt() { return publishedAt; }

    @Nullable
    public String getContent() { return content; }

    public boolean isEmbedded() { return embedded; }

    @NonNull
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setSource(@Nullable String source) { this.source = source; }
    public void setAuthor(@Nullable String author) { this.author = author; }
    public void setPublishedAt(@Nullable OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
    public void setContent(@Nullable String content) { this.content = content; }
    public void markEmbedded() { this.embedded = true; }
}
