package com.briefy.api;

import com.briefy.api.dto.ScheduleEventResponse;
import com.briefy.api.dto.ScheduleRequest;
import com.briefy.api.dto.ScheduleResponse;
import com.briefy.common.ApiResponse;
import com.briefy.domain.schedule.ScheduleService;
import com.briefy.infra.security.UserPrincipal;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok(scheduleService.create(principal.getUserId(), request));
    }

    @GetMapping
    public ApiResponse<List<ScheduleEventResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return ApiResponse.ok(scheduleService.listEvents(principal.getUserId(), from, to));
    }

    @GetMapping("/{id}")
    public ApiResponse<ScheduleResponse> getOne(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        return ApiResponse.ok(scheduleService.getOne(principal.getUserId(), id));
    }

    @PatchMapping("/{id}")
    public ApiResponse<ScheduleResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ScheduleRequest request) {
        return ApiResponse.ok(scheduleService.update(principal.getUserId(), id, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id) {
        scheduleService.delete(principal.getUserId(), id);
    }
}
