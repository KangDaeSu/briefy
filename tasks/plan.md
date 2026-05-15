# briefy 구현 계획

## 목표
사용자 관심사 기반 뉴스를 RAG로 요약하고, 개인 일정과 연동해 하루 브리핑을 제공하는 서비스.

---

## Phase 1 — 인프라 & 기초 설정
### 1-1. 프로젝트 초기화
- [x] Spring Boot 4.0 프로젝트 생성 (Maven, Java 21)
  - 의존성: Spring Web, Spring Data JPA, Spring AI, PostgreSQL Driver, Spring Security
- [x] React 19 + Vite + Javascript 프론트엔드 생성
- [x] Docker Compose: PostgreSQL 18 + pgvector 설정
- [x] GitHub 레포 및 브랜치 전략 수립 (`main` / `dev` / `feature/*`)

### 1-2. DB 스키마 설계
- [x] `users` — 사용자 계정 및 관심사 태그
- [x] `schedules` — 일정 (제목, 시간, 반복, 알림)
- [x] `news_articles` — 수집된 원문 기사 메타데이터
- [x] `news_embeddings` — 청크 임베딩 (pgvector)
- [x] `briefs` — 생성된 브리핑 캐시
- [x] Flyway 마이그레이션 스크립트 작성

### 1-3. Spring AI + pgvector 연동
- [x] `PgVectorStore` Bean 설정 (application.yml auto-config, Spring AI 2.0.0-M6)
- [x] `EmbeddingModel` 설정 (OpenAI text-embedding-3-large, 3072dim)
- [x] 임베딩 저장/검색 통합 테스트 작성 (Testcontainers + MockBean)

---

## Phase 2 — 핵심 도메인 구현
### 2-1. 일정 관리 (Schedule)
- [x] CRUD API: `POST/GET/PATCH/DELETE /api/v1/schedules`
- [x] 반복 일정 로직 (RRULE 기반, biweekly 0.6.8)
- [x] 일정 알림 스케줄러 (`@Scheduled`, 5분 주기 로그)
- [x] 프론트엔드: 캘린더 뷰 (자체 구현, 월간 그리드)

### 2-2. 뉴스 수집 파이프라인
- [x] News API 연동 (NewsAPI.org, RestClient)
- [x] 기사 청크 분할 전략 결정 (문단 기준, 512 토큰, TokenTextSplitter)
- [x] 임베딩 배치 처리 (`@Scheduled` + 배치 20건, 기사별 독립 트랜잭션)
- [x] 중복 기사 필터링 (SHA-256 URL 해시, DB UNIQUE 제약)

### 2-3. RAG 브리핑 생성
- [ ] `QuestionAnswerAdvisor` 또는 커스텀 RAG 체인 구현
- [ ] 사용자 관심사 → 쿼리 벡터 → 유사 뉴스 검색 (Top-K)
- [ ] 검색 결과 + 프롬프트 → ChatModel → 브리핑 생성
- [ ] 브리핑 캐시 (당일 생성 결과 재사용)
- [ ] Streaming 응답 지원 (SSE)

---

## Phase 3 — 사용자 기능
### 3-1. 인증/인가
- [ ] JWT 기반 로그인 (Spring Security 6)
- [ ] OAuth2 소셜 로그인 (Google)
- [ ] 프론트엔드 인증 상태 관리

### 3-2. 관심사 설정 UI
- [ ] 태그 기반 관심사 선택 화면
- [ ] 관심사 변경 시 브리핑 즉시 재생성 트리거

### 3-3. 브리핑 대시보드
- [ ] 오늘의 브리핑 뷰 (스트리밍 표시)
- [ ] 뉴스 원문 링크 및 출처 표시
- [ ] 일정 + 브리핑 통합 뷰 (홈 화면)

---

## Phase 4 — 품질 & 운영
### 4-1. 테스트
- [ ] 단위 테스트: 서비스 레이어 (Mockito)
- [ ] 통합 테스트: RAG 파이프라인 (Testcontainers + PostgreSQL)
- [ ] E2E 테스트: Playwright (핵심 사용자 플로우)

### 4-2. 성능
- [ ] pgvector HNSW 인덱스 파라미터 튜닝 (`m`, `ef_construction`)
- [ ] 임베딩 배치 크기 최적화
- [ ] 브리핑 캐시 TTL 전략

### 4-3. 배포
- [ ] Dockerfile (frontend, backend)
- [ ] Docker Compose (프로덕션 구성)
- [ ] CI/CD: GitHub Actions (빌드 → 테스트 → 배포)

---

## 현재 진행 상태

| Phase       | 상태      |
|-------------|-----------|
| Phase 1-1   | 완료      |
| Phase 1-2   | 완료      |
| Phase 1-3   | 완료      |
| Phase 2-1   | 완료      |
| Phase 2-2   | 완료      |
| Phase 2-3   | 미시작    |
| Phase 3     | 미시작    |
| Phase 4     | 미시작    |

> 마지막 업데이트: 2026-05-15
