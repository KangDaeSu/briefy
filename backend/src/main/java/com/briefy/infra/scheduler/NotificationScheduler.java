package com.briefy.infra.scheduler;

import com.briefy.domain.schedule.ScheduleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;

// Phase 3에서 실제 알림(푸시/이메일) 연동 예정
@Component
public class NotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(NotificationScheduler.class);

    private final ScheduleRepository scheduleRepository;

    public NotificationScheduler(ScheduleRepository scheduleRepository) {
        this.scheduleRepository = scheduleRepository;
    }

    @Scheduled(fixedDelay = 300_000) // 5분마다
    public void checkUpcomingSchedules() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime in30min = now.plusMinutes(30);
        var upcoming = scheduleRepository.findUpcoming(now, in30min);
        if (!upcoming.isEmpty()) {
            log.info("[알림] 30분 내 시작 일정 {}건", upcoming.size());
            upcoming.forEach(s ->
                log.info("  - [{}] '{}' @ {}", s.getId(), s.getTitle(), s.getStartTime())
            );
        }
    }
}
