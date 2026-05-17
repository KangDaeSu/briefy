-- 인증 제공자 컬럼 추가
-- provider: LOCAL(이메일/비밀번호), GOOGLE(OAuth2)
ALTER TABLE users
    ADD COLUMN provider    VARCHAR(20)  NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN provider_id VARCHAR(255);

-- 동일 제공자에서 중복 계정 방지
CREATE UNIQUE INDEX users_provider_id_idx
    ON users (provider, provider_id)
    WHERE provider_id IS NOT NULL;
