-- 개발용 테스트 사용자 (V7 이후 provider/provider_id 컬럼이 추가되므로 생략 가능)
-- Phase 3 인증 구현 완료 — 이 시드는 기존 데이터 호환용으로만 유지
INSERT INTO users (id, email, name, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'test@briefy.dev',
    'Test User',
    now(),
    now()
) ON CONFLICT DO NOTHING;
