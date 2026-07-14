package com.briefy.global.util;

import java.time.LocalDate;
import java.util.Set;

/**
 * 한국 공휴일 및 대체휴일 목록 (2024-2027).
 * 설날·추석·부처님오신날은 음력 기준으로 매년 날짜가 달라짐.
 * 선거일·임시공휴일은 그때그때 정부(국무회의)가 별도 지정하므로 매년 확인 필요.
 * 제헌절은 2008년 공휴일에서 제외됐다가 법률 개정으로 2026-05-11부터 재지정됨.
 */
public final class KoreanHolidays {

    private static final Set<LocalDate> HOLIDAYS = Set.of(
        // ── 2024 ──
        LocalDate.of(2024, 1, 1),   // 새해
        LocalDate.of(2024, 2, 9),   // 설날 연휴
        LocalDate.of(2024, 2, 10),  // 설날
        LocalDate.of(2024, 2, 11),  // 설날 연휴
        LocalDate.of(2024, 2, 12),  // 대체휴일
        LocalDate.of(2024, 3, 1),   // 삼일절
        LocalDate.of(2024, 4, 10),  // 22대 국회의원 총선거일
        LocalDate.of(2024, 5, 5),   // 어린이날
        LocalDate.of(2024, 5, 6),   // 대체휴일
        LocalDate.of(2024, 5, 15),  // 부처님오신날
        LocalDate.of(2024, 6, 6),   // 현충일
        LocalDate.of(2024, 8, 15),  // 광복절
        LocalDate.of(2024, 9, 16),  // 추석 연휴
        LocalDate.of(2024, 9, 17),  // 추석
        LocalDate.of(2024, 9, 18),  // 추석 연휴
        LocalDate.of(2024, 10, 1),  // 국군의날 (임시공휴일)
        LocalDate.of(2024, 10, 3),  // 개천절
        LocalDate.of(2024, 10, 9),  // 한글날
        LocalDate.of(2024, 12, 25), // 성탄절
        // ── 2025 ──
        LocalDate.of(2025, 1, 1),
        LocalDate.of(2025, 1, 27),  // 임시공휴일 (설 연휴 전날)
        LocalDate.of(2025, 1, 28),
        LocalDate.of(2025, 1, 29),
        LocalDate.of(2025, 1, 30),
        LocalDate.of(2025, 3, 1),
        LocalDate.of(2025, 5, 5),   // 어린이날 + 부처님오신날 겹침
        LocalDate.of(2025, 5, 6),   // 대체휴일
        LocalDate.of(2025, 6, 3),   // 21대 대통령선거일 (임시공휴일)
        LocalDate.of(2025, 6, 6),
        LocalDate.of(2025, 8, 15),
        LocalDate.of(2025, 10, 3),
        LocalDate.of(2025, 10, 5),
        LocalDate.of(2025, 10, 6),
        LocalDate.of(2025, 10, 7),
        LocalDate.of(2025, 10, 8),  // 대체휴일 (추석 연휴 일요일)
        LocalDate.of(2025, 10, 9),
        LocalDate.of(2025, 12, 25),
        // ── 2026 ──
        LocalDate.of(2026, 1, 1),
        LocalDate.of(2026, 2, 16),
        LocalDate.of(2026, 2, 17),
        LocalDate.of(2026, 2, 18),
        LocalDate.of(2026, 3, 1),
        LocalDate.of(2026, 3, 2),   // 대체휴일 (삼일절 일요일)
        LocalDate.of(2026, 5, 5),
        LocalDate.of(2026, 5, 24),  // 부처님오신날 (일요일)
        LocalDate.of(2026, 5, 25),  // 대체휴일
        LocalDate.of(2026, 6, 3),   // 전국동시지방선거일 (임시공휴일)
        LocalDate.of(2026, 6, 6),
        LocalDate.of(2026, 7, 17),  // 제헌절 (18년 만에 공휴일 재지정)
        LocalDate.of(2026, 8, 15),
        LocalDate.of(2026, 8, 17),  // 대체휴일 (광복절 토요일)
        LocalDate.of(2026, 9, 24),
        LocalDate.of(2026, 9, 25),
        LocalDate.of(2026, 9, 26),
        LocalDate.of(2026, 9, 28),  // 대체휴일 (추석 연휴 토요일)
        LocalDate.of(2026, 10, 3),
        LocalDate.of(2026, 10, 5),  // 대체휴일 (개천절 토요일)
        LocalDate.of(2026, 10, 9),
        LocalDate.of(2026, 12, 25),
        // ── 2027 ──
        LocalDate.of(2027, 1, 1),
        LocalDate.of(2027, 2, 6),
        LocalDate.of(2027, 2, 7),
        LocalDate.of(2027, 2, 8),
        LocalDate.of(2027, 3, 1),
        LocalDate.of(2027, 5, 5),
        LocalDate.of(2027, 5, 13),  // 부처님오신날
        LocalDate.of(2027, 6, 6),
        LocalDate.of(2027, 7, 17),  // 제헌절 (토요일)
        LocalDate.of(2027, 7, 19),  // 대체휴일 (제헌절 토요일, 미확정 예상치)
        LocalDate.of(2027, 8, 15),
        LocalDate.of(2027, 8, 16),  // 대체휴일 (광복절 일요일)
        LocalDate.of(2027, 9, 19),
        LocalDate.of(2027, 9, 20),
        LocalDate.of(2027, 9, 21),
        LocalDate.of(2027, 10, 3),
        LocalDate.of(2027, 10, 4),  // 대체휴일 (개천절 일요일)
        LocalDate.of(2027, 10, 9),
        LocalDate.of(2027, 10, 11), // 대체휴일 (한글날 토요일)
        LocalDate.of(2027, 12, 25)
    );

    private KoreanHolidays() {}

    public static boolean isHoliday(LocalDate date) {
        return HOLIDAYS.contains(date);
    }
}
