-- 생성된 브리핑 캐시
-- valid_until 이전이면 재사용, 이후면 재생성
CREATE TABLE briefs (
    id           UUID        NOT NULL DEFAULT uuid_generate_v7(),
    user_id      UUID        NOT NULL,
    content      TEXT        NOT NULL,
    -- 인용 출처: [{article_id, title, url, source}]
    sources      JSONB       NOT NULL DEFAULT '[]',
    generated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- 캐시 만료 시간 (통상 당일 자정)
    valid_until  TIMESTAMPTZ NOT NULL,

    CONSTRAINT briefs_pk      PRIMARY KEY (id),
    CONSTRAINT briefs_user_fk FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 유효한 최신 브리핑 조회용 인덱스
CREATE INDEX briefs_user_valid_idx ON briefs (user_id, valid_until DESC);
