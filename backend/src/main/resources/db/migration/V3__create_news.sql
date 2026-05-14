-- 수집된 뉴스 원문 메타데이터
CREATE TABLE news_articles (
    id           UUID         NOT NULL DEFAULT uuidv7(),
    url          TEXT         NOT NULL,
    -- SHA-256 hex — 중복 기사 필터링 기준 (URL 정규화 후 해시)
    url_hash     VARCHAR(64)  NOT NULL,
    title        TEXT         NOT NULL,
    source       VARCHAR(255),
    author       VARCHAR(255),
    published_at TIMESTAMPTZ,
    content      TEXT,
    -- 임베딩 처리 여부 (배치 처리 추적용)
    embedded     BOOLEAN      NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT news_articles_pk        PRIMARY KEY (id),
    CONSTRAINT news_articles_url_hash  UNIQUE (url_hash)
);

CREATE INDEX news_articles_published_idx ON news_articles (published_at DESC);
CREATE INDEX news_articles_embedded_idx  ON news_articles (embedded) WHERE embedded = false;

-- 뉴스 임베딩 저장소 (Spring AI PgVectorStore 호환 스키마)
-- Spring AI가 content/metadata/embedding 컬럼을 직접 관리
CREATE TABLE news_embeddings (
    id         UUID         NOT NULL DEFAULT uuidv7(),
    content    TEXT,
    -- article_id, title, url 등 참조 정보는 metadata JSONB에 저장
    metadata   JSONB,
    embedding  vector(3072),
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT news_embeddings_pk PRIMARY KEY (id)
);

-- HNSW 인덱스: vector(3072)는 2000차원 제한으로 halfvec 캐스팅 함수 인덱스 사용
-- pgvector 0.7.0+: halfvec 최대 4000차원, HNSW 검색 품질 유지
CREATE INDEX news_embeddings_hnsw_idx
    ON news_embeddings USING hnsw ((embedding::halfvec(3072)) halfvec_cosine_ops)
    WITH (m = 16, ef_construction = 64);
