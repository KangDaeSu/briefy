package com.briefy.api;

import com.briefy.api.dto.ScheduleEventResponse;
import com.briefy.api.dto.ScheduleRequest;
import com.briefy.api.dto.ScheduleResponse;
import com.briefy.api.dto.ScheduleUpdateRequest;
import com.briefy.common.ApiResponse;
import com.briefy.domain.schedule.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ScheduleResponse> create(
        @RequestHeader("X-User-Id") UUID userId,
        @Valid @RequestBody ScheduleRequest request
    ) {
        return ApiResponse.ok(scheduleService.create(userId, request));
    }

    @GetMapping
    public ApiResponse<List<ScheduleEventResponse>> list(
        @RequestHeader("X-User-Id") UUID userId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to
    ) {
        return ApiResponse.ok(scheduleService.listEvents(userId, from, to));
    }

    @GetMapping("/{id}")
    public ApiResponse<ScheduleResponse> getOne(
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable UUID id
    ) {
        return ApiResponse.ok(scheduleService.getOne(userId, id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ScheduleResponse> update(
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable UUID id,
        @Valid @RequestBody ScheduleUpdateRequest request
    ) {
        return ApiResponse.ok(scheduleService.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
        @RequestHeader("X-User-Id") UUID userId,
        @PathVariable UUID id
    ) {
        scheduleService.delete(userId, id);
    }
}
