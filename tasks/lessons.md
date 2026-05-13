# briefy — 삽질 기록 (Lessons Learned)

개발 중 마주친 문제, 해결 방법, 다음에 같은 실수를 반복하지 않기 위한 메모.

---

## 형식

```
### [날짜] 문제 제목
**증상**: 무슨 일이 일어났나
**원인**: 왜 그랬나
**해결**: 어떻게 고쳤나
**교훈**: 다음에는 어떻게 할 것인가
```

---

## Spring AI / pgvector

<!-- 예시 항목 — 실제 삽질 발생 시 추가 -->
<!--
### [2026-05-XX] PgVectorStore dimension mismatch
**증상**: 임베딩 저장 시 `ERROR: expected 1536 dimensions, not 3072`
**원인**: embedding 모델을 text-embedding-3-large(3072)로 바꿨는데 테이블 DDL은 1536 그대로
**해결**: `ALTER TABLE news_embeddings ALTER COLUMN embedding TYPE vector(3072)`
**교훈**: 모델 변경 시 반드시 Flyway 마이그레이션으로 컬럼 타입도 함께 변경
-->

---

## PostgreSQL / pgvector
### [2026-05-12] PostgreSQL 18 Docker 볼륨 경로 오류
### 증상
- briefy-db 컨테이너가 계속 restarting 상태
- "There appears to be PostgreSQL data in /var/lib/postgresql/data (unused mount/volume)" 에러

### 원인
- PostgreSQL 18부터 PGDATA 기본 경로가 변경됨
- 구버전: /var/lib/postgresql/data
- 신버전: /var/lib/postgresql/18/docker

### 해결
- docker-compose.yml 볼륨 마운트를 /var/lib/postgresql/data → /var/lib/postgresql 로 변경
- docker compose down -v 로 기존 볼륨 삭제 후 재시작

### 교훈
- PostgreSQL 18+ Docker 설정 시 볼륨은 반드시 /var/lib/postgresql 로 마운트
- 구버전 예제 그대로 복사하면 안 됨, 반드시 공식 문서 확인
---

## Spring Boot 4.0

---

## React 19

---

## 인프라 / 배포

---

## 기타
### [2026-05-13] Claude Code에 공식 문서 연결하기 (Context7 MCP)

**증상**: Claude Code가 React 19, Spring Boot 4.0 등 최신 API를 옛날 패턴으로 생성함 (예: forwardRef 사용, 구버전 Spring 어노테이션 등)

**원인**: Claude의 훈련 데이터 컷오프 이후 변경된 API를 실시간으로 알 수 없음

**해결**:
1. Context7 MCP 설치
```bash
   claude mcp add context7 -- npx -y @upstash/context7-mcp@latest
```
2. CLAUDE.md에 공식 문서 URL 명시
```markdown
   ## Official Documentation
   - React 19: https://react.dev
   - Spring Boot 4.0: https://docs.spring.io/spring-boot/docs/4.0.x/reference/html/
   - Spring AI: https://docs.spring.io/spring-ai/reference/
   - PostgreSQL 18: https://www.postgresql.org/docs/18/
   - pgvector: https://github.com/pgvector/pgvector
```
3. 프롬프트 끝에 `use context7` 추가하면 실시간 문서 기반으로 코드 생성

**교훈**:
- 최신 프레임워크 코드 생성 시 반드시 `use context7`를 붙일 것
- CLAUDE.md에 버전과 문서 URL을 명시해두면 매 세션마다 컨텍스트 재설정 불필요
- `claude mcp list`로 MCP 서버 등록 여부 먼저 확인
