-- OAuth2 제거에 따른 provider 관련 컬럼 삭제
DROP INDEX IF EXISTS users_provider_id_idx;
ALTER TABLE users DROP COLUMN IF EXISTS provider;
ALTER TABLE users DROP COLUMN IF EXISTS provider_id;
