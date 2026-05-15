-- 개발용 테스트 사용자 (Phase 3 인증 구현 전까지 사용)
INSERT INTO users (id, email, name, interests, created_at, updated_at)
VALUES (
    '11111111-1111-1111-1111-111111111111',
    'test@briefy.dev',
    'Test User',
    '["기술", "경제", "스포츠"]',
    now(),
    now()
) ON CONFLICT DO NOTHING;
