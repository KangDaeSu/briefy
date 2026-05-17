package com.briefy.domain.schedule;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    @Query("SELECT s FROM Schedule s JOIN FETCH s.user WHERE s.id = :id AND s.user.id = :userId")
    Optional<Schedule> findByIdAndUserId(
        @Param("id") UUID id,
        @Param("userId") UUID userId
    );

    @Query("""
        SELECT s FROM Schedule s
        WHERE s.user.id = :userId
        AND s.rrule IS NULL
        AND s.startTime < :rangeEnd
        AND s.endTime > :rangeStart
        ORDER BY s.startTime
        """)
    List<Schedule> findNonRecurringByUserAndRange(
        @Param("userId") UUID userId,
        @Param("rangeStart") OffsetDateTime rangeStart,
        @Param("rangeEnd") OffsetDateTime rangeEnd
    );

    // 기간 내 발생 가능성이 있는 반복 일정: startTime이 rangeEnd 이전인 것들
    @Query("""
        SELECT s FROM Schedule s
        WHERE s.user.id = :userId
        AND s.rrule IS NOT NULL
        AND s.startTime < :rangeEnd
        ORDER BY s.startTime
        """)
    List<Schedule> findRecurringByUser(
        @Param("userId") UUID userId,
        @Param("rangeEnd") OffsetDateTime rangeEnd
    );

    // 알림 스케줄러용: 특정 시간대에 시작하는 비반복 일정
    @Query("""
        SELECT s FROM Schedule s
        WHERE s.rrule IS NULL
        AND s.startTime >= :from
        AND s.startTime < :to
        ORDER BY s.startTime
        """)
    List<Schedule> findUpcoming(
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to,
        Pageable pageable
    );
}
