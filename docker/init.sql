-- pgvector 확장 활성화
CREATE EXTENSION IF NOT EXISTS vector;

-- PostgreSQL 18 네이티브 UUIDv7 확인 (별도 확장 불필요)
-- uuid_generate_v7() 는 PG18 빌트인
