package com.briefy.api.dto;

import com.briefy.domain.schedule.Schedule;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ScheduleResponse(
    UUID id,
    UUID userId,
    String title,
    @Nullable String description,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    @Nullable String rrule,
    boolean recurring,
    OffsetDateTime createdAt,
    OffsetDateTime updatedAt
) {
    public static ScheduleResponse from(Schedule s) {
        return new ScheduleResponse(
            s.getId(),
            s.getUser().getId(),
            s.getTitle(),
            s.getDescription(),
            s.getStartTime(),
            s.getEndTime(),
            s.getRrule(),
            s.getRrule() != null,
            s.getCreatedAt(),
            s.getUpdatedAt()
        );
    }
}
