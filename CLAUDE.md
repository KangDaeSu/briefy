# briefy

React 19 + Spring Boot 4.0 기반 일정 관리 서비스.
반복 일정(RRULE), 일정 중복 방지, 알림 스케줄러를 제공한다.

## Tech Stack

| Layer     | Technology                          |
|-----------|-------------------------------------|
| Frontend  | React 19, JavaScript (JSX), Vite 8  |
| Backend   | Spring Boot 4.0.6                   |
| Database  | PostgreSQL 18                       |
| Migration | Flyway                              |
| Testing   | Testcontainers 2.0.5, JUnit 5       |

## Project Structure

이 프로젝트는 **도메인형 패키지 구조(Feature-based Package Structure)**를 엄격하게 따릅니다. 코드는 크게 비즈니스 로직을 담는 `domain`과 공통 설정을 담는 `global`로 나뉩니다.

```text
briefy/
├── frontend/                       # React 19 SPA (Vite)
│   ├── src/
│   │   ├── api/
│   │   │   ├── client.js           # fetch 래퍼 (get/post/patch/delete)
│   │   │   └── schedules.js        # 일정 API 함수
│   │   ├── components/
│   │   │   ├── MonthCalendar.jsx
│   │   │   └── ScheduleModal.jsx
│   │   └── pages/
│   │       └── CalendarPage.jsx
│   └── package.json
├── backend/                        # Spring Boot 4.0 서버
│   ├── src/main/java/com/briefy/
│   │   ├── BriefyApplication.java
│   │   ├── domain/                 # ⭐️ 비즈니스 도메인별 패키지 (MVC 통합)
│   │   │   ├── user/
│   │   │   │   ├── controller/
│   │   │   │   ├── dto/
│   │   │   │   ├── entity/
│   │   │   │   ├── repository/
│   │   │   │   └── service/
│   │   │   └── schedule/
│   │   │       ├── controller/     # ScheduleController.java
│   │   │       ├── dto/            # request, response DTOs
│   │   │       ├── entity/         # Schedule.java
│   │   │       ├── repository/
│   │   │       └── service/
│   │   └── global/                 # ⭐️ 전역 공통/인프라 설정 (기존 common, infra 병합)
│   │       ├── config/             # SecurityConfig.java 등
│   │       ├── dto/                # ApiResponse.java (공통 응답 wrapper)
│   │       ├── error/              # BriefyErrorCode, BriefyException, GlobalExceptionHandler
│   │       └── scheduler/          # NotificationScheduler (@Scheduled)
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/               # Flyway V1, V2, V5
│   └── pom.xml
└── tasks/
    ├── plan.md                     # 구현 계획
    ├── lessons.md                  # 삽질 기록
    └── skills.md                   # 이 프로젝트 작업 방법 가이드
## Commands

### Frontend
```bash
cd frontend
npm install
npm run dev          # 개발 서버 (localhost:5173)
npm run build
npm run lint         # ESLint
```

### Backend
```bash
cd backend
./mvnw spring-boot:run   # 개발 서버 (localhost:8080)
./mvnw test
./mvnw package
```

### Database
```bash
# Docker Compose로 PostgreSQL 18 기동
docker compose up -d

# DB 초기화 (스키마 변경 시)
docker compose down -v && docker compose up -d
```

## Key Conventions

### API Design
- REST 엔드포인트: `@RequestMapping("/api/v1/...")` 경로 접두사로 버전 관리
- 모든 응답은 `ApiResponse<T>` record wrapper 사용: `ApiResponse.ok(data)`, `ApiResponse.error(errorCode)`
- 에러 코드는 `BriefyErrorCode` enum으로 관리 (코드 + 메시지 쌍)
- 현재 인증 미구현(Phase 3) — `X-User-Id` 헤더로 임시 사용자 식별

### Domain Layer
- JPA Entity: 기본 생성자 `protected`, 공개 생성자로 필수 필드 강제, 도메인 메서드(update 등) 포함
- PK: `@Id @UuidGenerator(style = UuidGenerator.Style.TIME)` — Hibernate 6.x UUIDv7 생성
- Service: 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`
- Null 안전성: 모든 필드/파라미터/리턴값에 `@NonNull` / `@Nullable` (org.jspecify) 명시

### Repeat Schedule (RRULE)
- iCalendar RRULE 형식으로 저장 (예: `FREQ=WEEKLY;BYDAY=MO,WE,FR`)
- `biweekly` 0.6.8 라이브러리로 파싱 및 발생 일시 확장 (`ScheduleService.expandRrule`)
- 범위 조회 시 비반복 일정 + 반복 일정 확장 후 합산, `startTime` 기준 정렬

### Database Conventions
- **UUIDv7**: PG18 네이티브 함수는 `uuidv7()` — `uuid_generate_v7()`는 PG18에 없음
- **일정 중복 방지**: GiST EXCLUDE 제약 (`tstzrange(start_time, end_time, '[)') WITH &&`)
  `btree_gist` extension 필수 (V1 마이그레이션에서 활성화)
- **스키마 검증**: `ddl-auto: validate` — `CHAR(n)` 대신 `VARCHAR(n)` 사용 (bpchar vs varchar 불일치 방지)

### Spring Boot 4.0 Conventions
- `@MockBean` / `@SpyBean` 제거됨 → `@MockitoBean` / `@MockitoSpyBean` 사용
  (`org.springframework.test.context.bean.override.mockito`)
- Web starter: `spring-boot-starter-webmvc`
- Flyway starter: `spring-boot-starter-flyway` + `flyway-database-postgresql` 별도 추가
- Java 21 + Virtual Threads (Loom) 기본 활성화

### Testcontainers 2.x Conventions
- 아티팩트: `testcontainers-postgresql` (1.x의 `postgresql`과 다름)
- JUnit 5 연동: `testcontainers-junit-jupiter` 별도 아티팩트 추가 필수
- Spring Boot 통합: `spring-boot-testcontainers` starter + `@ServiceConnection`

### Frontend Conventions
- 프레임워크: Vite SPA (Next.js/RSC 없음) — 모든 페이지는 CSR
- 언어: JavaScript (JSX) — TypeScript 미도입
- 데이터 페칭: `useState` + `useEffect` + `fetch` 래퍼 (`src/api/client.js`)
- 상태 관리: `useState` (외부 상태 라이브러리 없음)
- API 베이스: `VITE_API_BASE_URL` env 변수
- 인증 헤더: `X-User-Id: <UUID>` (Phase 3 JWT 구현 전 임시)

## Environment Variables

### Backend (`backend/.env`)
```
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/briefy
SPRING_DATASOURCE_USERNAME=briefy
SPRING_DATASOURCE_PASSWORD=briefy

# JWT: Base64-encoded 256-bit 이상 랜덤 키 (개발용 기본값이 있으나 프로덕션에서 교체 필수)
JWT_SECRET=<base64-encoded-256bit-key>

# 프론트엔드 URL (CORS 대상)
FRONTEND_URL=http://localhost:5173
```

### Frontend (`frontend/.env.local`)
```
VITE_API_BASE_URL=http://localhost:8080
```

## Reference Docs

코드 작업 시 아래 공식 문서를 Context7 MCP 또는 WebFetch로 직접 참조한다.

| 기술                           | 문서 URL                                                             |
|------------------------------|----------------------------------------------------------------------|
| Spring Boot 4.0              | https://docs.spring.io/spring-boot/reference/                        |
| Spring Boot — Testcontainers | https://docs.spring.io/spring-boot/reference/testing/testcontainers.html |
| Flyway                       | https://documentation.red-gate.com/flyway                           |
| React 19                     | https://react.dev                                                    |
| Vite                         | https://vite.dev/guide/                                              |
| jspecify                     | https://jspecify.dev/docs/user-guide                                 |
| PostgreSQL 18                | https://www.postgresql.org/docs/18/                                  |

## Development Notes

- **최신 버전 우선**: Spring Boot 4.0은 이전 버전과 API가 달라진 곳이 많다. 코드 작성 전 공식 문서 확인 또는 `use context7`로 최신 API 기준으로 생성
- `ddl-auto: validate` — Flyway가 스키마 관리, Hibernate는 검증만 수행
- 마이그레이션: V1, V2, V5, V7, V8, V9. DB 초기화 필요 시 `docker compose down -v`
