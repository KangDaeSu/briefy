-- RAG 기능 제거 후 남은 dead column 정리
ALTER TABLE users DROP COLUMN IF EXISTS interests;

-- V5 시드 유저 제거: password_hash 없고 provider_id 없어 로그인 불가
-- schedules FK가 ON DELETE CASCADE이므로 연관 일정도 함께 삭제
DELETE FROM users WHERE id = '11111111-1111-1111-1111-111111111111';
