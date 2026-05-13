package com.briefy.domain.brief;

import com.briefy.domain.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.NonNull;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "briefs")
public class Brief {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @NonNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NonNull
    @Column(nullable = false, columnDefinition = "text")
    private String content;

    // [{article_id, title, url, source}] — 인용 출처 목록
    @NonNull
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<BriefSource> sources = new ArrayList<>();

    @NonNull
    @Column(name = "generated_at", nullable = false, updatable = false)
    private OffsetDateTime generatedAt;

    // 당일 캐시: 보통 당일 자정까지
    @NonNull
    @Column(name = "valid_until", nullable = false)
    private OffsetDateTime validUntil;

    protected Brief() {
    }

    public Brief(@NonNull User user, @NonNull String content,
                 @NonNull List<BriefSource> sources, @NonNull OffsetDateTime validUntil) {
        this.user = user;
        this.content = content;
        this.sources = new ArrayList<>(sources);
        this.validUntil = validUntil;
    }

    @PrePersist
    private void prePersist() {
        generatedAt = OffsetDateTime.now();
    }

    public boolean isExpired() {
        return OffsetDateTime.now().isAfter(validUntil);
    }

    public UUID getId() { return id; }

    @NonNull
    public User getUser() { return user; }

    @NonNull
    public String getContent() { return content; }

    @NonNull
    public List<BriefSource> getSources() { return sources; }

    @NonNull
    public OffsetDateTime getGeneratedAt() { return generatedAt; }

    @NonNull
    public OffsetDateTime getValidUntil() { return validUntil; }

    public record BriefSource(
            @NonNull String articleId,
            @NonNull String title,
            @NonNull String url,
            @NonNull String source
    ) {
    }
}
