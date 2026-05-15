package com.briefy.domain.schedule;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import com.briefy.api.dto.ScheduleEventResponse;
import com.briefy.api.dto.ScheduleRequest;
import com.briefy.api.dto.ScheduleResponse;
import com.briefy.api.dto.ScheduleUpdateRequest;
import com.briefy.common.BriefyErrorCode;
import com.briefy.common.exception.BriefyException;
import com.briefy.domain.user.UserRepository;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ScheduleService {

    private static final Logger log = LoggerFactory.getLogger(ScheduleService.class);
    private static final int RRULE_OCCURRENCE_LIMIT = 500;
    private static final DateTimeFormatter ICAL_DATE_FMT =
        DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    public ScheduleService(ScheduleRepository scheduleRepository, UserRepository userRepository) {
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ScheduleResponse create(UUID userId, ScheduleRequest req) {
        var user = userRepository.findById(userId)
            .orElseThrow(() -> new BriefyException(BriefyErrorCode.USER_NOT_FOUND));
        var schedule = new Schedule(user, req.title(), req.startTime(), req.endTime());
        schedule.update(req.title(), req.description(), req.startTime(), req.endTime(), req.rrule());
        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    public ScheduleResponse getOne(UUID userId, UUID scheduleId) {
        var schedule = findOwnedSchedule(userId, scheduleId);
        return ScheduleResponse.from(schedule);
    }

    public List<ScheduleEventResponse> listEvents(UUID userId, OffsetDateTime from, OffsetDateTime to) {
        List<ScheduleEventResponse> events = new ArrayList<>();

        // 비반복 일정: 범위와 겹치는 것들
        scheduleRepository.findNonRecurringByUserAndRange(userId, from, to)
            .forEach(s -> events.add(ScheduleEventResponse.from(s)));

        // 반복 일정: RRULE 확장
        scheduleRepository.findRecurringByUser(userId, to).forEach(s -> {
            Duration duration = Duration.between(s.getStartTime(), s.getEndTime());
            expandRrule(s.getRrule(), s.getStartTime(), from, to).forEach(occ ->
                events.add(ScheduleEventResponse.occurrence(s, occ, occ.plus(duration)))
            );
        });

        events.sort(Comparator.comparing(ScheduleEventResponse::startTime));
        return events;
    }

    @Transactional
    public ScheduleResponse update(UUID userId, UUID scheduleId, ScheduleUpdateRequest req) {
        var schedule = findOwnedSchedule(userId, scheduleId);
        schedule.update(req.title(), req.description(), req.startTime(), req.endTime(), req.rrule());
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(UUID userId, UUID scheduleId) {
        var schedule = findOwnedSchedule(userId, scheduleId);
        scheduleRepository.delete(schedule);
    }

    private Schedule findOwnedSchedule(UUID userId, UUID scheduleId) {
        var schedule = scheduleRepository.findById(scheduleId)
            .orElseThrow(() -> new BriefyException(BriefyErrorCode.SCHEDULE_NOT_FOUND));
        if (!schedule.getUser().getId().equals(userId)) {
            throw new BriefyException(BriefyErrorCode.FORBIDDEN);
        }
        return schedule;
    }

    private List<OffsetDateTime> expandRrule(@Nullable String rrule, OffsetDateTime dtStart,
                                             OffsetDateTime rangeStart, OffsetDateTime rangeEnd) {
        if (rrule == null) return List.of();
        try {
            String dtStartStr = dtStart.withOffsetSameInstant(ZoneOffset.UTC).format(ICAL_DATE_FMT);
            String ics = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\n" +
                         "BEGIN:VEVENT\r\n" +
                         "DTSTART:" + dtStartStr + "\r\n" +
                         "RRULE:" + rrule + "\r\n" +
                         "END:VEVENT\r\n" +
                         "END:VCALENDAR";

            ICalendar ical = Biweekly.parse(ics).first();
            VEvent event = ical.getEvents().get(0);
            var it = event.getDateIterator(TimeZone.getTimeZone("UTC"));

            List<OffsetDateTime> occurrences = new ArrayList<>();
            int count = 0;
            while (it.hasNext() && count++ < RRULE_OCCURRENCE_LIMIT) {
                Date occ = it.next();
                OffsetDateTime odt = occ.toInstant().atOffset(ZoneOffset.UTC);
                if (odt.isAfter(rangeEnd)) break;
                if (!odt.isBefore(rangeStart)) {
                    occurrences.add(odt);
                }
            }
            return occurrences;
        } catch (Exception e) {
            log.warn("RRULE 파싱 실패 (rrule={}): {}", rrule, e.getMessage());
            return List.of();
        }
    }
}
