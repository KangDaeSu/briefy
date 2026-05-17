package com.briefy.domain.schedule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;

public record ScheduleRequest(
    @NotBlank String title,
    @Nullable String description,
    @NotNull OffsetDateTime startTime,
    @NotNull OffsetDateTime endTime,
    @Nullable String rrule
) {}
