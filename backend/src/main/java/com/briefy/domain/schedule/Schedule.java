package com.briefy.domain.schedule;

import com.briefy.domain.user.User;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "schedules")
public class Schedule {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @NonNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NonNull
    @Column(nullable = false)
    private String title;

    @Nullable
    @Column(columnDefinition = "text")
    private String description;

    @NonNull
    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @NonNull
    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    // iCalendar RRULE 형식 (예: "FREQ=WEEKLY;BYDAY=MO,WE,FR")
    @Nullable
    @Column(length = 500)
    private String rrule;

    @NonNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @NonNull
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Schedule() {
    }

    public Schedule(@NonNull User user, @NonNull String title,
                    @NonNull OffsetDateTime startTime, @NonNull OffsetDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        this.user = user;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
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
    public User getUser() { return user; }

    @NonNull
    public String getTitle() { return title; }

    @Nullable
    public String getDescription() { return description; }

    @NonNull
    public OffsetDateTime getStartTime() { return startTime; }

    @NonNull
    public OffsetDateTime getEndTime() { return endTime; }

    @Nullable
    public String getRrule() { return rrule; }

    @NonNull
    public OffsetDateTime getCreatedAt() { return createdAt; }

    @NonNull
    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void update(@NonNull String title, @Nullable String description,
                       @NonNull OffsetDateTime startTime, @NonNull OffsetDateTime endTime,
                       @Nullable String rrule) {
        if (!endTime.isAfter(startTime)) {
            throw new IllegalArgumentException("endTime must be after startTime");
        }
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.rrule = rrule;
    }
}
