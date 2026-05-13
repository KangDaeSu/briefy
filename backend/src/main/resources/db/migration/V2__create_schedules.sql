-- 일정 테이블
-- 중복 방지: GiST EXCLUDE 제약 — 동일 user_id의 시간 범위 겹침 방지
-- (PostgreSQL 17+ WITHOUT OVERLAPS 구문과 동일한 의미)
CREATE TABLE schedules (
    id          UUID         NOT NULL DEFAULT uuid_generate_v7(),
    user_id     UUID         NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    start_time  TIMESTAMPTZ  NOT NULL,
    end_time    TIMESTAMPTZ  NOT NULL,
    -- iCalendar RRULE 형식 반복 규칙 (예: FREQ=WEEKLY;BYDAY=MO,WE,FR)
    rrule       VARCHAR(500),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT schedules_pk             PRIMARY KEY (id),
    CONSTRAINT schedules_user_fk        FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT schedules_end_after_start CHECK (end_time > start_time),
    -- 동일 사용자의 일정 시간 겹침 방지 (DB 엔진 레벨 보장)
    CONSTRAINT schedules_no_overlap     EXCLUDE USING gist (
        user_id WITH =,
        tstzrange(start_time, end_time, '[)') WITH &&
    )
);

CREATE INDEX schedules_user_time_idx ON schedules (user_id, start_time, end_time);
