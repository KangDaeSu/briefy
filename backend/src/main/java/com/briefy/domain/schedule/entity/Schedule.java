package com.briefy.domain.schedule.entity;

import com.briefy.domain.user.entity.User;
import com.briefy.global.error.BriefyErrorCode;
import com.briefy.global.error.BriefyException;
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
@Table(name = "schedules")
@EntityListeners(AuditingEntityListener.class)
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

    @Column(name = "skip_holidays", nullable = false)
    private boolean skipHolidays = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    protected Schedule() {
    }

    public Schedule(@NonNull User user, @NonNull String title, @Nullable String description,
                    @NonNull OffsetDateTime startTime, @NonNull OffsetDateTime endTime,
                    @Nullable String rrule, boolean skipHolidays) {
        if (!endTime.isAfter(startTime)) {
            throw new BriefyException(BriefyErrorCode.SCHEDULE_INVALID_TIME);
        }
        this.user = user;
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.rrule = rrule;
        this.skipHolidays = skipHolidays;
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

    public boolean isSkipHolidays() { return skipHolidays; }

    public OffsetDateTime getCreatedAt() { return createdAt; }

    public OffsetDateTime getUpdatedAt() { return updatedAt; }

    public void update(@NonNull String title, @Nullable String description,
                       @NonNull OffsetDateTime startTime, @NonNull OffsetDateTime endTime,
                       @Nullable String rrule, boolean skipHolidays) {
        if (!endTime.isAfter(startTime)) {
            throw new BriefyException(BriefyErrorCode.SCHEDULE_INVALID_TIME);
        }
        this.title = title;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.rrule = rrule;
        this.skipHolidays = skipHolidays;
    }
}
