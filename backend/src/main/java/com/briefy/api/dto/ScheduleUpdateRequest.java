package com.briefy.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

import java.time.OffsetDateTime;

public record ScheduleUpdateRequest(
    @NotBlank String title,
    @Nullable String description,
    @NotNull OffsetDateTime startTime,
    @NotNull OffsetDateTime endTime,
    @Nullable String rrule
) {}
