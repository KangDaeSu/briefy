# briefy 구현 계획

## 목표
React 19 + Spring Boot 4.0 기반 일정 관리 서비스.
반복 일정(RRULE), 일정 중복 방지, 알림 스케줄러를 제공한다.

---

## Phase 1 — 인프라 & 기초 설정
### 1-1. 프로젝트 초기화
- [x] Spring Boot 4.0 프로젝트 생성 (Maven, Java 21)
- [x] React 19 + Vite + JavaScript 프론트엔드 생성
- [x] Docker Compose: PostgreSQL 18 설정
- [x] GitHub 레포 및 브랜치 전략 수립 (`main` / `dev` / `feature/*`)

### 1-2. DB 스키마 설계
- [x] `users` — 사용자 계정
- [x] `schedules` — 일정 (제목, 시간, 반복 RRULE, 알림)
- [x] Flyway 마이그레이션 스크립트 작성 (V1, V2, V5)

---

## Phase 2 — 핵심 도메인 구현
### 2-1. 일정 관리 (Schedule)
- [x] CRUD API: `POST/GET/PATCH/DELETE /api/v1/schedules`
- [x] 반복 일정 로직 (RRULE 기반, biweekly 0.6.8)
- [x] 일정 알림 스케줄러 (`@Scheduled`, 5분 주기 로그)
- [x] 프론트엔드: 캘린더 뷰 (자체 구현, 월간 그리드)

---

## Phase 3 — 사용자 기능
### 3-1. 인증/인가
- [x] JWT 기반 로그인 (이메일/비밀번호, httpOnly 쿠키)
- [x] OAuth2 소셜 로그인 (Google)
- [x] 프론트엔드 인증 상태 관리 (AuthContext, ProtectedRoute)
- [x] 로그인/회원가입 페이지

### 3-2. 사용자 프로필
- [x] GET/PATCH/DELETE /api/v1/users/me (이름 수정, 계정 삭제)
- [x] 프로필 설정 UI (/settings — 이름 수정, 계정 삭제)

---

## Phase 4 — 품질 & 운영
### 4-1. 테스트
- [x] 단위 테스트: 서비스 레이어 (Mockito)
  - `AuthServiceTest` — 6개 (register/login 정상·예외)
  - `UserServiceTest` — 4개 (findById/updateName/delete)
  - `ScheduleServiceTest` — 9개 (CRUD + RRULE 확장)
- [x] 통합 테스트: Schedule CRUD (Testcontainers + PostgreSQL 18)
  - `ScheduleApiIntegrationTest` — 5개 (HTTP 전체 플로우 + JWT 쿠키)
  - 전체 25개 테스트 모두 통과 (`mvn clean test`)
- [x] E2E 테스트: Playwright (핵심 사용자 플로우)
  - `tests/auth.spec.js` — 4개 (회원가입, 로그인, 오류, 미인증 리다이렉트)
  - `tests/calendar.spec.js` — 4개 (일정 생성·수정·삭제, 월 이동)

### 4-2. 배포
- [x] `backend/Dockerfile` — 멀티스테이지 (Maven → JRE 21 Alpine)
- [x] `frontend/Dockerfile` — 멀티스테이지 (Node → Nginx Alpine)
- [x] `frontend/nginx.conf` — SPA 폴백 + `/api/` 리버스 프록시
- [x] `docker-compose.prod.yml` — 프로덕션 스택 (db + backend + frontend)
- [x] `.github/workflows/ci.yml` — 3-job CI
  - `backend`: `mvn test` (Testcontainers)
  - `frontend`: ESLint + Vite 빌드
  - `e2e`: backend·frontend 서버 기동 후 Playwright 실행

### 코드 품질 개선 (Phase 4 진행 중 병행)
- [x] `UserService` 신규 추가 — 컨트롤러에서 Repository 직접 참조 제거
- [x] `JwtProvider.tryParse()` — 토큰 이중 파싱 제거
- [x] `ScheduleRepository.findByIdAndUserId` — JOIN FETCH로 N+1 해결
- [x] `Schedule` 생성자 통합 — 이중 초기화 패턴 제거
- [x] `OAuth2SuccessHandler` 사일런트 실패 수정
- [x] `CustomOAuth2UserService` 명시적 save 추가
- [x] Flyway V8 — 미사용 `interests` 컬럼 및 시드 사용자 정리
- [x] 프론트엔드 ESLint 경고 전체 해소

---

## 현재 진행 상태

| Phase           | 상태  |
|-----------------|-------|
| Phase 1-1       | 완료  |
| Phase 1-2       | 완료  |
| Phase 2-1       | 완료  |
| Phase 3-1       | 완료  |
| Phase 3-2       | 완료  |
| Phase 4-1 테스트 | 완료  |
| Phase 4-2 배포   | 완료  |

> 마지막 업데이트: 2026-05-17 (Phase 4 전체 완료)
