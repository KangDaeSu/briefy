-- pgvector: vector 타입 및 유사도 연산자 제공
CREATE EXTENSION IF NOT EXISTS vector;
-- btree_gist: UUID 컬럼을 GiST 인덱스에 포함하기 위해 필요 (schedules 중복 방지 제약)
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- PK: uuidv7() — PG18 빌트인, 시간순 정렬 가능 (B-tree 단편화 최소화)
CREATE TABLE users (
    id            UUID        NOT NULL DEFAULT uuidv7(),
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255),
    name          VARCHAR(100) NOT NULL,
    -- 관심사 태그 목록 (예: ["AI", "경제", "스포츠"])
    interests     JSONB       NOT NULL DEFAULT '[]',
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT now(),

    CONSTRAINT users_pk          PRIMARY KEY (id),
    CONSTRAINT users_email_unique UNIQUE (email)
);

CREATE INDEX users_email_idx ON users (email);
