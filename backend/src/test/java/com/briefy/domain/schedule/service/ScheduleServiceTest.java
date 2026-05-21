package com.briefy.domain.schedule.service;

import com.briefy.domain.schedule.dto.ScheduleRequest;
import com.briefy.domain.schedule.entity.Schedule;
import com.briefy.domain.schedule.repository.ScheduleRepository;
import com.briefy.domain.user.entity.User;
import com.briefy.domain.user.service.UserService;
import com.briefy.global.error.BriefyErrorCode;
import com.briefy.global.error.BriefyException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    @Mock ScheduleRepository scheduleRepository;
    @Mock UserService userService;
    @InjectMocks ScheduleService scheduleService;

    private UUID userId;
    private User user;
    private UUID scheduleId;
    private Schedule schedule;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = new User("u@test.com", "User");
        ReflectionTestUtils.setField(user, "id", userId);

        scheduleId = UUID.randomUUID();
        startTime = OffsetDateTime.now().plusDays(1).withNano(0);
        endTime = startTime.plusHours(1);
        schedule = new Schedule(user, "스탠드업", null, startTime, endTime, null, false);
        ReflectionTestUtils.setField(schedule, "id", scheduleId);
    }

    // ─── create ───────────────────────────────────────────────────────────────

    @Test
    void create_success() {
        when(userService.findById(userId)).thenReturn(user);
        when(scheduleRepository.save(any())).thenReturn(schedule);

        var req = new ScheduleRequest("스탠드업", null, startTime, endTime, null, null);
        var result = scheduleService.create(userId, req);

        assertThat(result.title()).isEqualTo("스탠드업");
        assertThat(result.recurring()).isFalse();
    }

    @Test
    void create_userNotFound_throws() {
        when(userService.findById(any())).thenThrow(new BriefyException(BriefyErrorCode.USER_NOT_FOUND));

        var req = new ScheduleRequest("스탠드업", null, startTime, endTime, null, null);

        assertThatThrownBy(() -> scheduleService.create(userId, req))
                .isInstanceOf(BriefyException.class)
                .satisfies(e -> assertThat(((BriefyException) e).getErrorCode())
                        .isEqualTo(BriefyErrorCode.USER_NOT_FOUND));
    }

    // ─── getOne ───────────────────────────────────────────────────────────────

    @Test
    void getOne_success() {
        when(scheduleRepository.findByIdAndUserId(scheduleId, userId))
                .thenReturn(Optional.of(schedule));

        var result = scheduleService.getOne(userId, scheduleId);

        assertThat(result.title()).isEqualTo("스탠드업");
        assertThat(result.id()).isEqualTo(scheduleId);
    }

    @Test
    void getOne_scheduleNotFound_throws() {
        when(scheduleRepository.findByIdAndUserId(scheduleId, userId)).thenReturn(Optional.empty());
        when(scheduleRepository.existsById(scheduleId)).thenReturn(false);

        assertThatThrownBy(() -> scheduleService.getOne(userId, scheduleId))
                .isInstanceOf(BriefyException.class)
                .satisfies(e -> assertThat(((BriefyException) e).getErrorCode())
                        .isEqualTo(BriefyErrorCode.SCHEDULE_NOT_FOUND));
    }

    @Test
    void getOne_forbidden_throws() {
        when(scheduleRepository.findByIdAndUserId(scheduleId, userId)).thenReturn(Optional.empty());
        when(scheduleRepository.existsById(scheduleId)).thenReturn(true);

        assertThatThrownBy(() -> scheduleService.getOne(userId, scheduleId))
                .isInstanceOf(BriefyException.class)
                .satisfies(e -> assertThat(((BriefyException) e).getErrorCode())
                        .isEqualTo(BriefyErrorCode.FORBIDDEN));
    }

    // ─── update ───────────────────────────────────────────────────────────────

    @Test
    void update_success() {
        when(scheduleRepository.findByIdAndUserId(scheduleId, userId))
                .thenReturn(Optional.of(schedule));

        var req = new ScheduleRequest("수정된 제목", "설명", startTime, endTime, null, null);
        var result = scheduleService.update(userId, scheduleId, req);

        assertThat(result.title()).isEqualTo("수정된 제목");
        assertThat(result.description()).isEqualTo("설명");
    }

    // ─── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_success() {
        when(scheduleRepository.findByIdAndUserId(scheduleId, userId))
                .thenReturn(Optional.of(schedule));

        scheduleService.delete(userId, scheduleId);

        verify(scheduleRepository).delete(schedule);
    }

    // ─── listEvents ───────────────────────────────────────────────────────────

    @Test
    void listEvents_nonRecurring_returnsEvents() {
        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = from.plusMonths(1);

        when(scheduleRepository.findNonRecurringByUserAndRange(userId, from, to))
                .thenReturn(List.of(schedule));
        when(scheduleRepository.findRecurringByUser(userId, to))
                .thenReturn(List.of());

        var events = scheduleService.listEvents(userId, from, to);

        assertThat(events).hasSize(1);
        assertThat(events.get(0).title()).isEqualTo("스탠드업");
        assertThat(events.get(0).recurring()).isFalse();
        assertThat(events.get(0).id()).isEqualTo(scheduleId);
    }

    @Test
    void listEvents_recurring_expandsOccurrences() {
        OffsetDateTime from = OffsetDateTime.now();
        OffsetDateTime to = from.plusMonths(1);

        // 매주 월요일 반복
        Schedule recurring = new Schedule(user, "주간 회의", null,
                startTime, endTime, "FREQ=WEEKLY;BYDAY=MO", false);
        ReflectionTestUtils.setField(recurring, "id", UUID.randomUUID());

        when(scheduleRepository.findNonRecurringByUserAndRange(userId, from, to))
                .thenReturn(List.of());
        when(scheduleRepository.findRecurringByUser(userId, to))
                .thenReturn(List.of(recurring));

        var events = scheduleService.listEvents(userId, from, to);

        // 한 달 안에 최소 3번의 월요일이 있어야 함
        assertThat(events.size()).isGreaterThanOrEqualTo(3);
        assertThat(events).allMatch(e -> e.recurring());
    }
}
