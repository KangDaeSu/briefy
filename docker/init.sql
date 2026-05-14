-- pgvector 확장 활성화
CREATE EXTENSION IF NOT EXISTS vector;

-- PostgreSQL 18 네이티브 UUIDv7: uuidv7() 함수 사용 (별도 확장 불필요)
-- uuid_generate_v7()는 pg_uuidv7 확장 이름이며 PG18 빌트인이 아님
