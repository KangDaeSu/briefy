package com.briefy.domain.schedule.service;

import biweekly.Biweekly;
import biweekly.ICalendar;
import biweekly.component.VEvent;
import com.briefy.domain.schedule.dto.ScheduleEventResponse;
import com.briefy.domain.schedule.dto.ScheduleRequest;
import com.briefy.domain.schedule.dto.ScheduleResponse;
import com.briefy.domain.schedule.entity.Schedule;
import com.briefy.domain.schedule.repository.ScheduleRepository;
import com.briefy.domain.user.service.UserService;
import com.briefy.global.error.BriefyErrorCode;
import com.briefy.global.error.BriefyException;
import com.briefy.global.util.KoreanHolidays;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneId;
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
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    private final ScheduleRepository scheduleRepository;
    private final UserService userService;

    public ScheduleService(ScheduleRepository scheduleRepository, UserService userService) {
        this.scheduleRepository = scheduleRepository;
        this.userService = userService;
    }

    @Transactional
    public ScheduleResponse create(UUID userId, ScheduleRequest req) {
        var user = userService.findById(userId);
        var schedule = new Schedule(user, req.title(), req.description(),
                req.startTime(), req.endTime(), req.rrule(), req.skipHolidays());
        return ScheduleResponse.from(scheduleRepository.save(schedule));
    }

    public ScheduleResponse getOne(UUID userId, UUID scheduleId) {
        return ScheduleResponse.from(findOwnedSchedule(userId, scheduleId));
    }

    public List<ScheduleEventResponse> listEvents(UUID userId, OffsetDateTime from, OffsetDateTime to) {
        List<ScheduleEventResponse> events = new ArrayList<>();

        scheduleRepository.findNonRecurringByUserAndRange(userId, from, to)
            .forEach(s -> events.add(ScheduleEventResponse.from(s)));

        scheduleRepository.findRecurringByUser(userId, to).forEach(s -> {
            Duration duration = Duration.between(s.getStartTime(), s.getEndTime());
            expandRrule(s.getRrule(), s.getStartTime(), from, to, s.isSkipHolidays()).forEach(occ ->
                events.add(ScheduleEventResponse.occurrence(s, occ, occ.plus(duration)))
            );
        });

        events.sort(Comparator.comparing(ScheduleEventResponse::startTime));
        return events;
    }

    @Transactional
    public ScheduleResponse update(UUID userId, UUID scheduleId, ScheduleRequest req) {
        var schedule = findOwnedSchedule(userId, scheduleId);
        schedule.update(req.title(), req.description(), req.startTime(), req.endTime(),
                req.rrule(), req.skipHolidays());
        return ScheduleResponse.from(schedule);
    }

    @Transactional
    public void delete(UUID userId, UUID scheduleId) {
        scheduleRepository.delete(findOwnedSchedule(userId, scheduleId));
    }

    private Schedule findOwnedSchedule(UUID userId, UUID scheduleId) {
        return scheduleRepository.findByIdAndUserId(scheduleId, userId).orElseThrow(() -> {
            BriefyErrorCode code = scheduleRepository.existsById(scheduleId)
                    ? BriefyErrorCode.FORBIDDEN
                    : BriefyErrorCode.SCHEDULE_NOT_FOUND;
            return new BriefyException(code);
        });
    }

    private List<OffsetDateTime> expandRrule(@Nullable String rrule, OffsetDateTime dtStart,
                                             OffsetDateTime rangeStart, OffsetDateTime rangeEnd,
                                             boolean skipHolidays) {
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
                if (odt.isBefore(rangeStart)) continue;
                if (skipHolidays && KoreanHolidays.isHoliday(odt.atZoneSameInstant(KST).toLocalDate())) continue;
                occurrences.add(odt);
            }
            return occurrences;
        } catch (Exception e) {
            log.warn("RRULE 파싱 실패 (rrule={}): {}", rrule, e.getMessage());
            return List.of();
        }
    }
}
