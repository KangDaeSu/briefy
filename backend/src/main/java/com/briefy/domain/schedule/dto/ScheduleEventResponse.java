package com.briefy.domain.schedule.dto;

import com.briefy.domain.schedule.entity.Schedule;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * 날짜 범위 조회 시 반환하는 이벤트 단위 DTO.
 * 반복 일정은 각 발생 시점마다 별도의 이벤트로 확장된다.
 */
public record ScheduleEventResponse(
    UUID id,
    String title,
    @Nullable String description,
    OffsetDateTime startTime,
    OffsetDateTime endTime,
    @Nullable String rrule,
    boolean recurring,
    boolean skipHolidays
) {
    public static ScheduleEventResponse from(Schedule s) {
        return new ScheduleEventResponse(
            s.getId(), s.getTitle(), s.getDescription(),
            s.getStartTime(), s.getEndTime(), s.getRrule(), false, s.isSkipHolidays()
        );
    }

    public static ScheduleEventResponse occurrence(Schedule s, OffsetDateTime start, OffsetDateTime end) {
        return new ScheduleEventResponse(
            s.getId(), s.getTitle(), s.getDescription(),
            start, end, s.getRrule(), true, s.isSkipHolidays()
        );
    }
}
