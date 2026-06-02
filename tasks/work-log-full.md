# briefy 전체 작업 일지

커밋 타임스탬프 기준으로 정리한 전체 작업 기록.

---

## 2026-05-12 (월)

| 시작 | 종료 | 작업 | 커밋 |
|------|------|------|------|
| 02:04 | — | 프로젝트 초기 세팅 | `45a7baf` |

### 02:04 | 프로젝트 초기 세팅

- `CLAUDE.md` 작성 — 기술 스택, 컨벤션, 레퍼런스 문서 정의
- `docker-compose.yml` — PostgreSQL 18 + pgvector 0.8.2 컨테이너 설정
- `.gitignore`, 루트 구조 세팅

---

## 2026-05-13 (화)

| 시작 | 종료 | 작업 | 커밋 |
|------|------|------|------|
| 22:38 | — | Spring Boot 4.0 백엔드 초기 세팅 | `2a79573` |

### 22:38 | Spring Boot 4.0 백엔드 초기 세팅 (Phase 1)

- Spring Boot 4.0.6 프로젝트 생성 (`pom.xml`, 의존성)
- 도메인형 패키지 구조 설계 (`domain/`, `global/`)
- `user` 도메인 — Entity, Repository, Service, Controller, DTO
- `schedule` 도메인 — Entity, Repository, Service, Controller, DTO
- Flyway 마이그레이션 V1 (btree_gist, GiST EXCLUDE 중복 방지)
- `SecurityConfig`, `JwtProvider`, `JwtAuthenticationFilter`
- `ApiResponse` 공통 응답 래퍼, `BriefyErrorCode`, `GlobalExceptionHandler`
- `application.yml` 기본 설정

---

## 2026-05-14 (수)

| 시작 | 종료 | 작업 | 커밋 |
|------|------|------|------|
| 23:53 | — | React 19 + Vite 프론트엔드 초기 세팅 | `0149160` |

### 23:53 | React 19 + Vite 프론트엔드 초기 세팅 (Phase 1-1)

- Vite 8 + React 19 프로젝트 생성
- `api/client.js` — fetch 래퍼 (get / post / patch / delete)
- `MonthCalendar.jsx` — 월간 캘린더 컴포넌트
- `ScheduleModal.jsx` — 일정 생성/수정/삭제 모달
- `CalendarPage.jsx` — 메인 캘린더 페이지
- `VITE_API_BASE_URL` 환경변수 기반 API 연결

---

## 2026-05-15 (목)

| 시작 | 종료 | 작업 | 커밋 |
|------|------|------|------|
| 01:27 | 01:29 | dev 브랜치 생성 및 `.claude/` gitignore 추가 | `cf7a08f`, `88cc71b` |
| 01:50 | 02:08 | Spring AI 2.0.0-M6 호환성 수정 + Testcontainers 2.x 업그레이드 | `80af190`, `847db2b` |
| 02:08 | 02:31 | 테스트 전체 통과 + lessons.md 삽질 기록 5건 추가 | `0d3fb87`, `2802ff4` |

### 01:27 → 01:29 | 브랜치 전략 수립 및 gitignore 정리

- `dev` 브랜치 생성, 브랜치 전략 문서화
- `.claude/` 디렉터리 gitignore 추가

### 01:50 → 02:08 | Spring AI 2.0.0-M6 호환성 수정

- `QuestionAnswerAdvisor` 패키지 이동 대응 (`advisor` → `advisor.vectorstore`)
- 생성자 제거 → Builder 패턴으로 전환
- Testcontainers 1.x → 2.0.5 업그레이드
  - 아티팩트명 변경: `postgresql` → `testcontainers-postgresql`
  - `testcontainers-junit-jupiter` 별도 추가
- `fetch` 클라이언트 개선

### 02:08 → 02:31 | 테스트 통과 및 삽질 기록

- CHAR(n) → VARCHAR(n) 변경 (Hibernate `ddl-auto: validate` bpchar 불일치 해결)
- `@MockBean` → `@MockitoBean` 전환 (Spring Boot 4.0 breaking change)
- 인터페이스 default 메서드 Mock 이슈 → `@TestConfiguration` 실구현체로 교체
- PG18 UUIDv7 함수명 `uuid_generate_v7()` → `uuidv7()` 수정
- `lessons.md` 삽질 기록 5건 추가

---

## 2026-05-16 (금)

| 시작 | 종료 | 작업 | 커밋 |
|------|------|------|------|
| 02:06 | — | Phase 2-1 일정 관리 + Phase 2-2 뉴스 수집 파이프라인 구현 | `26d7714` |

### 02:06 | Phase 2 핵심 기능 구현

- 반복 일정(RRULE) 확장 로직 — `biweekly` 0.6.8 라이브러리 연동
- 범위 조회 시 비반복 + 반복 일정 합산 후 `startTime` 기준 정렬
- 일정 중복 방지 GiST EXCLUDE 제약 검증
- `NotificationScheduler` — `@Scheduled` 기반 알림 스케줄러
- 뉴스 수집 파이프라인 (Phase 2-2)

---

## 2026-05-17 (토)

| 시작 | 종료 | 작업 | 커밋 |
|------|------|------|------|
| 17:09 | — | 프로젝트 일정 변경에 따른 파일 수정 | `9bb428c` |
| 20:55 | 21:01 | 패키지 구조 재편, OAuth 제거, 보안 설정 강화, UI 추가 + 배포 설정 | `4feaa1a`, `db269e5` |
| 21:13 | 21:44 | Railway 빌드 오류 해결 및 배포 안정화 | `2e69a35`, `22cf69d` |
| 22:39 | 22:55 | JWT 환경변수 호환성 수정 | `600e5de`, `ec30ebf` |
| 23:13 | 23:29 | DB 설정 수정 및 JWT 시크릿 SHA-256 파생으로 전환 | `16b8979`, `9e92bb3`, `6dfb3bd`, `bbd1566` |
| 23:56 | — | Railway/Vercel 배포 삽질 기록 추가 | `6feefe1` |

### 17:09 | 프로젝트 일정 변경

- `plan.md` 등 프로젝트 관련 파일 일정 반영 수정

### 20:55 → 21:01 | 패키지 구조 재편 + 배포 설정 추가

- `common/`, `infra/` 패키지 → `global/`로 병합
- OAuth 관련 코드 제거
- 사용자 인증 강화 (JWT 기반 완전 전환)
- `ScheduleModal`, `MonthCalendar` UI 기능 추가
- `railway.toml`, `Dockerfile` 추가 — Railway 배포 설정
- Vercel `vercel.json` 추가

### 21:13 → 21:44 | Railway 배포 오류 연속 수정

- `railway.toml` 루트 이동 (서브디렉터리 인식 불가 문제)
- `dockerfilePath`, `COPY` 경로 수정 (빌드 컨텍스트가 루트)
- `server.port: ${PORT:8080}` — Railway 주입 포트 환경변수 대응
- JWT_SECRET 미설정 NPE 해결 — Railway Variables에 추가
- DB URL `postgresql://` → `jdbc:postgresql://` 형식 수정
- `VITE_API_BASE_URL` `https://` 누락 수정 (Vercel 상대경로 해석 오류)

### 22:39 → 22:55 | JWT 환경변수 호환성

- JWT_SECRET 값 공백/줄바꿈 `trim()` 처리
- `Decoders.BASE64` → `Decoders.BASE64URL` 전환 (URL-safe 문자 `_`, `-` 허용)

### 23:13 → 23:29 | DB 설정 및 JWT 키 안정화

- PostgreSQL 18 Docker 볼륨 경로 수정 (`/var/lib/postgresql/data` → `/var/lib/postgresql`)
- `.env` 파일 임포트 비활성화 — Railway 환경변수 직접 주입 방식으로 통일
- JWT 시크릿을 SHA-256 해시로 파생 — 입력 길이·포맷 무관하게 항상 256-bit 키 생성
- JWT 디버그 출력 제거

### 23:56 | 배포 삽질 기록 정리

- `lessons.md`에 Railway/Vercel 배포 오류 7건 추가

---

## 2026-05-18 (일)

| 시작 | 종료 | 작업 | 커밋 |
|------|------|------|------|
| 18:03 | 18:17 | 크로스오리진 401 수정 (Bearer 헤더 전환) | `3a4e7f9`, `9485540` |
| 18:42 | 18:52 | 모달 시간 입력 UX 개선 (오전/오후 select, RRULE 프리셋) | `24251ff`, `bc6917a` |
| 18:52 | 19:01 | 시간/분 입력 범위 제한 (24초과·60이상 방지) | `dd8d4b1` |
| 19:01 | 19:27 | 날짜·시간 한 줄 배치, 24시 날짜 이월, 오전/오후 드롭다운 | `7c2f778`, `e137d5f` |
| 19:19 | 19:44 | 단일 박스 통합, 달력 아이콘, datetime-local 피커 | `c59d82e`, `9d972ed` |
| 19:44 | 19:53 | 날짜·시간 간격 축소 | `081f454`, `5bc6923` |
| 19:53 | 19:56 | lessons.md 오류 기록 정리 | `505743d` |

### 18:03 → 18:17 | 크로스오리진 인증 401 수정

- SameSite=Lax 쿠키 차단 문제 근본 해결 — Bearer 헤더 방식으로 전환
- `AuthResponse` record 추가 — 응답 body에 JWT 토큰 포함
- `JwtAuthenticationFilter` — Authorization 헤더 우선 추출, 쿠키 폴백
- `client.js` — `setToken()` 추가, 모든 요청에 `Authorization: Bearer` 헤더 첨부
- `AuthContext.jsx` — 로그인/회원가입/로그아웃 시 토큰 localStorage 관리

### 18:17 → 18:42 | CORS Preflight 403 수정

- `SecurityConfig` — `OPTIONS /**` permitAll 추가
- `setAllowedOrigins` → `setAllowedOriginPatterns` 교체
- `allowedHeaders`에 `Authorization` 명시 추가

### 18:42 → 18:52 | 일정 모달 시간 입력 UX 1차 개선

- 오전/오후 토글 버튼 → `<select>` 로 교체
- 13~23 입력 시 자동 오후 변환 (13 → 오후 1시, 23 → 오후 11시), 24 → 오전 0시
- RRULE 드롭다운 프리셋 추가 (매일 / 매주 / 격주 / 매월 / 매년 / 주중 매일 / 직접 입력)

### 18:52 → 19:01 | 시간·분 입력 범위 제한

- `type="number"` → `type="text" inputMode="numeric"` 전환 (React 클램핑 문제 해결)
- 시간 최대 24, 분 최대 59 클램핑 + blur 시 자동 포맷

### 19:01 → 19:44 | 날짜·시간 UI 전면 개편

- 날짜·시간 한 줄 배치 (`datetime-box` 단일 컨테이너)
- 24 입력 후 blur → 날짜 하루 이월 + 오전 0시 설정
- 오전/오후 커스텀 드롭다운 구현 → overflow:hidden 클리핑 문제로 네이티브 select로 재교체
- 네이티브 달력 아이콘 숨기고 SVG 달력 버튼 우측 고정
- 숨긴 `datetime-local` input + `showPicker()` — 달력 버튼으로 날짜·시간·오전오후 통합 피커
- CSS 특이성 충돌 (`.modal-form input` > `.time-hour-input`) → `.datetime-box .time-hour-input` + `!important` 해결

### 19:44 → 19:53 | 날짜·시간 간격 축소

- `flex: 1` → `flex: 0 0 auto` 변경 — date input이 남은 공간 전부 차지하던 문제 해결
- date input 우측 패딩 4px → 0

### 19:53 → 19:56 | lessons.md 오류 기록 정리

- 크로스오리진 401 / CORS 403 / localStorage 오염 3건 추가

---

## 날짜별 작업량 요약

| 날짜 | 커밋 수 | 주요 작업 |
|------|---------|-----------|
| 2026-05-12 | 1 | 프로젝트 초기 세팅 |
| 2026-05-13 | 1 | Spring Boot 4.0 백엔드 세팅 |
| 2026-05-14 | 1 | React 19 프론트엔드 세팅 |
| 2026-05-15 | 6 | 호환성 수정, 테스트 통과, 삽질 기록 |
| 2026-05-16 | 1 | Phase 2 핵심 기능 (일정 관리 + 뉴스 파이프라인) |
| 2026-05-17 | 9 | 패키지 재편, Railway/Vercel 배포 전투 |
| 2026-05-18 | 12 | 인증 오류 수정, 모달 시간 입력 UI 전면 개편 |
| **합계** | **31** | |
