# Briefy 코드베이스 가이드

처음 코드를 보는 사람을 위한 전체 흐름 설명서.

---

## 1. 프로젝트 개요

**Briefy**는 월간 캘린더 기반 일정 관리 서비스다.

| 항목 | 내용 |
|---|---|
| 프론트엔드 | React 19 + Vite (CSR SPA, `localhost:5173`) |
| 백엔드 | Spring Boot 4.0 REST API (`localhost:8080`) |
| DB | PostgreSQL 18 (Docker Compose로 실행) |
| 인증 | JWT (httpOnly 쿠키 + `Authorization: Bearer` 헤더 이중 지원) |
| 반복 일정 | iCalendar RRULE 형식 (`biweekly` 라이브러리로 파싱) |

---

## 2. 전체 아키텍처 흐름

```
브라우저 (React SPA)
  │
  │  HTTP (fetch, JWT 포함)
  ▼
Spring Boot API (:8080)
  │  JwtAuthenticationFilter → SecurityContextHolder
  │
  ├─ AuthController   /api/v1/auth/**
  ├─ UserController   /api/v1/users/**
  └─ ScheduleController /api/v1/schedules/**
        │
        ▼
    ScheduleService / AuthService / UserService
        │
        ▼
    JPA Repository → PostgreSQL 18
```

---

## 3. 백엔드 패키지 구조

```
com.briefy/
├── BriefyApplication.java          # 진입점. @EnableJpaAuditing 포함
├── domain/                         # 비즈니스 도메인 (MVC 통합)
│   ├── user/
│   │   ├── controller/             # AuthController, UserController
│   │   ├── dto/                    # 요청/응답 DTO (Java record)
│   │   ├── entity/                 # User, PasswordResetToken (JPA)
│   │   ├── repository/             # UserRepository, PasswordResetTokenRepository
│   │   └── service/                # AuthService, UserService, PasswordResetService
│   └── schedule/
│       ├── controller/             # ScheduleController
│       ├── dto/                    # ScheduleRequest, ScheduleResponse, ScheduleEventResponse
│       ├── entity/                 # Schedule (JPA)
│       ├── repository/             # ScheduleRepository
│       └── service/                # ScheduleService (RRULE 확장 포함)
└── global/                         # 전역 인프라/설정
    ├── config/
    │   ├── SecurityConfig.java     # Spring Security 필터 체인
    │   ├── JwtProvider.java        # JWT 생성/파싱
    │   ├── JwtAuthenticationFilter.java  # 요청마다 JWT 검증
    │   ├── JwtProperties.java      # application.yml jwt.* 바인딩
    │   ├── CookieProperties.java   # cookie.* 설정 바인딩
    │   └── UserPrincipal.java      # SecurityContext에 저장되는 인증 주체
    ├── dto/
    │   └── ApiResponse.java        # 공통 응답 래퍼 { success, data, message }
    ├── error/
    │   ├── BriefyErrorCode.java    # 에러 코드 enum (S001, U001, A001 …)
    │   ├── BriefyException.java    # 도메인 예외 (RuntimeException)
    │   └── GlobalExceptionHandler.java  # @RestControllerAdvice
    ├── scheduler/
    │   └── NotificationScheduler.java   # 5분마다 30분 내 일정 로그 (알림 미구현)
    └── util/
        └── KoreanHolidays.java     # 공휴일 판단 (skipHolidays 옵션)
```

**패키지 원칙:** 기능(도메인)별로 묶는다. `UserController`와 `UserService`는 같은 `user/` 폴더 안에 있다. 계층별(controller/, service/, …)로 분리하지 않는다.

---

## 4. 공통 응답 형식

모든 API 응답은 `ApiResponse<T>` 레코드로 감싼다.

```json
// 성공
{ "success": true, "data": { ... }, "message": null }

// 실패
{ "success": false, "data": null, "message": "일정을 찾을 수 없습니다" }
```

`ApiResponse.ok(data)` / `ApiResponse.error(errorCode)` 두 팩토리 메서드만 쓴다.

---

## 5. 인증 흐름

### 5-1. 로그인/회원가입

```
[프론트] authApi.login({ email, password })
  → POST /api/v1/auth/login
  ← 응답: { success, data: { user, token } }
       + Set-Cookie: jwt=<token>; HttpOnly

[프론트] setToken(token)          → localStorage.setItem('jwt', token)
[프론트] setUser(res.data.user)   → AuthContext 상태 업데이트
```

JWT는 **쿠키(HttpOnly)** 와 **localStorage** 두 곳에 동시에 저장된다.
- 쿠키: 서버가 `Set-Cookie`로 내려줌 (7일 만료)
- localStorage: `setToken()`으로 프론트가 직접 저장 → 이후 요청 시 `Authorization: Bearer <token>` 헤더로 전송

### 5-2. 요청마다 JWT 검증 (백엔드)

`JwtAuthenticationFilter`가 모든 요청을 가로챈다.

```
HTTP 요청 도착
  ↓
JwtAuthenticationFilter.doFilterInternal()
  ├─ Authorization 헤더에서 Bearer 토큰 추출 (없으면)
  └─ jwt 쿠키에서 토큰 추출
       ↓
  JwtProvider.tryParse(token)
       ↓ (성공 시)
  UserPrincipal 생성 (userId, email)
       ↓
  SecurityContextHolder에 인증 객체 저장
       ↓
  컨트롤러의 @AuthenticationPrincipal UserPrincipal 로 주입됨
```

### 5-3. 인증이 필요 없는 엔드포인트

`SecurityConfig`에서 허용 목록을 관리한다.
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register`
- `POST /api/v1/auth/logout`
- `GET /actuator/health`
- `GET /actuator/info`

이 외 모든 요청은 유효한 JWT가 없으면 401 반환. `/actuator/env`·`/actuator/heapdump` 같은 민감한 엔드포인트는 인증 없이 노출되면 안 되므로, 안전한 두 개만 명시적으로 허용한다.

### 5-4. 앱 시작 시 인증 상태 복원

```
App 마운트
  → AuthProvider useEffect
  → authApi.me() → GET /api/v1/auth/me
  → (성공) setUser(user)
  → (실패) setUser(null)  // 만료·없음
```

`ProtectedRoute`는 `user === null && !loading`이면 `/login`으로 리다이렉트.

---

## 6. 일정(Schedule) 흐름

### 6-1. 캘린더 화면 렌더링

```
CalendarPage 마운트
  → year, month 상태 (기본: 현재 월)
  → fetchEvents(year, month)
      → schedulesApi.list(from, to)
      → GET /api/v1/schedules?from=...&to=...
      ← List<ScheduleEventResponse>
  → events 상태 저장
  → MonthCalendar에 events 전달 → 그리드에 점/바 표시
```

날짜 경계: `new Date(y, m, 1)`로 로컬 타임 기준 ISO 문자열을 만든다. `fieldsToISO`도 로컬 타임 기준이므로 두 값이 동일한 기준을 쓴다 — KST(UTC+9) 등 비UTC 환경에서 월 경계가 일치하지 않으면 해당 월 이벤트가 조회 범위 밖으로 빠진다.

### 6-2. 백엔드의 일정 목록 조회 (반복 일정 처리 포함)

```java
ScheduleService.listEvents(userId, from, to)
  ├─ 1. 비반복 일정 조회
  │      findNonRecurringByUserAndRange(userId, from, to)
  │      → rrule IS NULL 이고 범위 겹치는 것
  │
  └─ 2. 반복 일정 확장
         findRecurringByUser(userId, to)  ← startTime < rangeEnd 인 것 전체
         → 각 일정마다 expandRrule() 호출
              → biweekly로 RRULE 파싱
              → rangeStart ~ rangeEnd 범위 안에 드는 발생일 추출
              → skipHolidays 옵션이면 KoreanHolidays로 걸러냄
         → ScheduleEventResponse.occurrence(s, occ, occEnd) 생성

  최종: 두 목록 합쳐 startTime 오름차순 정렬
```

### 6-3. 일정 생성/수정

```
[프론트] ScheduleModal에서 폼 작성 → onSave(data)
  → schedulesApi.create(data) / schedulesApi.update(id, data)
  → POST/PATCH /api/v1/schedules[/:id]

[백엔드] ScheduleController
  → scheduleService.create(userId, req)
  → new Schedule(user, title, ...) → 생성자에서 endTime > startTime 검증
  → scheduleRepository.save(schedule)
  → DB의 GiST EXCLUDE 제약이 중복 시간을 막음 (S002 에러)
```

### 6-4. 반복 일정 데이터 형식

RRULE은 iCalendar 표준 문자열로 DB에 저장된다.

```
FREQ=WEEKLY;BYDAY=MO,WE,FR   → 매주 월·수·금
FREQ=DAILY                    → 매일
FREQ=MONTHLY                  → 매월 같은 날
```

저장 시엔 단일 행 (원본 startTime, endTime, rrule).
조회 시엔 `expandRrule()`이 발생일마다 `ScheduleEventResponse`를 생성.

---

## 7. 프론트엔드 구조

```
src/
├── main.jsx                  # ReactDOM.createRoot → <App />
├── App.jsx                   # BrowserRouter + AuthProvider + 라우팅
├── contexts/
│   └── AuthContext.jsx       # user, loading, login/logout/register/updateUser/deleteAccount
├── api/
│   ├── client.js             # fetch 래퍼 (api.get/post/patch/delete)
│   ├── auth.js               # authApi (login, register, logout, me …)
│   ├── schedules.js          # schedulesApi (list, getOne, create, update, delete, search)
│   └── users.js              # usersApi (updateMe, deleteMe)
├── components/
│   ├── ProtectedRoute.jsx    # 미인증이면 /login 리다이렉트
│   ├── MonthCalendar.jsx     # 월간 그리드 (날짜별 이벤트 점 표시)
│   ├── ScheduleModal.jsx     # 일정 생성/수정 모달 폼
│   └── YearMonthPicker.jsx   # 연·월 선택 드롭다운
├── pages/
│   ├── CalendarPage.jsx      # 메인 화면 (상태 관리, 검색, 날짜 패널)
│   ├── LoginPage.jsx
│   ├── RegisterPage.jsx
│   └── SettingsPage.jsx      # 이름 수정, 계정 삭제
├── hooks/
│   └── useDarkMode.js        # 다크모드 토글 (localStorage)
└── utils/
    ├── savedAccounts.js      # 최근 로그인 계정 목록 (localStorage)
    └── holidays.js           # 프론트 공휴일 표시용
```

### 라우팅 구조

```
/          → CalendarPage  (ProtectedRoute)
/settings  → SettingsPage  (ProtectedRoute)
/login     → LoginPage
/register  → RegisterPage
*          → / 로 리다이렉트
```

### API 호출 패턴

`client.js`의 `api` 객체가 모든 fetch를 처리한다.

- JWT가 localStorage에 있으면 자동으로 `Authorization: Bearer <token>` 헤더 추가
- `credentials: 'include'`로 쿠키도 함께 전송
- HTTP 오류 시 `{ status, body }` 포함한 Error 객체를 throw
- 204 응답은 null 반환

---

## 8. DB 스키마 요약

### users 테이블
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | UUID | PK, `uuidv7()` 자동 생성 |
| email | VARCHAR(255) | UNIQUE |
| password_hash | VARCHAR(255) | BCrypt 해시, null 허용 |
| name | VARCHAR(100) | 표시 이름 |
| created_at / updated_at | TIMESTAMPTZ | 자동 관리 |

### schedules 테이블
| 컬럼 | 타입 | 설명 |
|---|---|---|
| id | UUID | PK, UUIDv7 |
| user_id | UUID | FK → users (CASCADE DELETE) |
| title | VARCHAR(255) | 필수 |
| description | TEXT | 선택 |
| start_time / end_time | TIMESTAMPTZ | 시작·종료 시각 |
| rrule | VARCHAR(500) | iCalendar RRULE, null이면 비반복 |
| skip_holidays | BOOLEAN | 공휴일 건너뛰기 |
| color | VARCHAR(7) | 색상 코드 (예: `#3b82f6`) |

**핵심 제약:** GiST EXCLUDE로 동일 사용자의 시간 겹침을 DB 레벨에서 방지.
```sql
CONSTRAINT schedules_no_overlap EXCLUDE USING gist (
    user_id WITH =,
    tstzrange(start_time, end_time, '[)') WITH &&
)
```
이 제약이 위반되면 `S002 SCHEDULE_OVERLAP` 에러가 프론트까지 전달된다.

### Flyway 마이그레이션 이력
| 버전 | 내용 |
|---|---|
| V1 | `btree_gist` 확장, `users` 테이블 |
| V2 | `schedules` 테이블 + GiST EXCLUDE |
| V5 | 테스트 시드 유저 |
| V7 | OAuth2 provider 컬럼 추가 |
| V8 | `interests` 컬럼 및 시드 제거 |
| V9 | provider 컬럼 제거 |
| V10 | `skip_holidays` 컬럼 추가 |
| V11 | `password_reset_tokens` 테이블 |
| V12 | `color` 컬럼 추가 |

---

## 9. 에러 처리 구조

### 백엔드

```
도메인 로직에서 BriefyException(BriefyErrorCode.XXX) throw
  ↓
GlobalExceptionHandler (@RestControllerAdvice)
  ↓
ApiResponse.error(errorCode) → HTTP 4xx 응답
```

에러 코드 목록 (`BriefyErrorCode`):

| 코드 | 의미 |
|---|---|
| S001 | 일정 없음 |
| S002 | 일정 시간 겹침 |
| S003 | 종료 시간 < 시작 시간 |
| U001 | 사용자 없음 |
| U002 | 이메일 중복 |
| A001 | 인증 필요 |
| A002 | 권한 없음 |
| A003/A005 | JWT 유효하지 않음 |
| A004 | 이메일/비밀번호 불일치 |

### 프론트엔드

`api.client.js`에서 HTTP 오류를 catch해 `err.body.message`를 꺼낸다.
각 폼(ScheduleModal, LoginPage 등)에서 `catch (err) { setError(err.body?.message) }`로 UI에 표시.

---

## 10. ScheduleModal 입력 처리 특이사항

시간 입력은 브라우저 호환성을 위해 직접 구현했다.

- **표시**: 12시간제 (오전/오후 + 시/분 텍스트 입력)
- **내부 저장**: 날짜 문자열 + hour/minute/ampm 분리
- **전송**: `fieldsToISO(date, hour, minute, ampm)` → UTC ISO 8601 문자열

```
입력 "25" (시) → handleHourBlur → 13 이상이면 오후로 자동 전환
입력 "12" (시) → handleHourChange → 오전/오후 자동 토글
입력 "24" (시) → handleHourBlur → 다음 날 00:00으로 날짜 이동
```

캘린더 아이콘을 누르면 숨겨진 `<input type="datetime-local">`의 `showPicker()`를 호출해 네이티브 달력을 열고, 선택 값을 시간 필드에 역방향으로 반영한다.

---

## 11. 주요 개발 규칙 요약

| 항목 | 규칙 |
|---|---|
| PK | `@UuidGenerator(style = TIME)` → UUIDv7 (시간순 정렬 가능) |
| 트랜잭션 | 클래스에 `@Transactional(readOnly=true)`, 쓰기 메서드에만 `@Transactional` |
| Null 안전 | 모든 파라미터/리턴에 `@NonNull` / `@Nullable` (jspecify) 명시 |
| 응답 | 모든 컨트롤러 리턴 타입은 `ApiResponse<T>` |
| 인증 주입 | `@AuthenticationPrincipal UserPrincipal principal` |
| 소유권 검증 | `findOwnedSchedule(userId, scheduleId)` — 없거나 남 것이면 모두 404 (IDOR 방지) |
| DB 스키마 | Flyway 관리, Hibernate는 `validate` 모드 (DDL 변경 금지) |
| Spring Boot 4 | `@MockBean` → `@MockitoBean` (패키지도 달라짐) |

---

## 12. 로컬 개발 시작 순서

```bash
# 1. DB 기동
docker compose up -d

# 2. 백엔드
cd backend
./mvnw spring-boot:run

# 3. 프론트엔드
cd frontend
npm install
npm run dev
```

`frontend/.env.local`에 `VITE_API_BASE_URL=http://localhost:8080` 필요.
`backend/.env`에 DB 접속 정보 필요 (기본값: briefy/briefy).

DB 스키마를 바꿔야 하면:
```bash
docker compose down -v && docker compose up -d
```
Flyway가 재기동 시 자동으로 마이그레이션을 순서대로 적용한다.

---

## 13. JWT 보안 — 배포 환경 고려사항

JWT를 `HttpOnly` 쿠키에만 저장하면 XSS로부터 안전하지만, **Railway(백엔드) + Vercel(프론트엔드)** 교차 출처 배포에서는 쿠키가 동작하지 않는 문제가 있다.

| 항목 | 값 |
|---|---|
| 백엔드 | `something.railway.app` |
| 프론트엔드 | `something.vercel.app` |
| 쿠키 도메인 | `railway.app` — 브라우저가 서드파티로 분류 |

브라우저는 `railway.app`이 내린 쿠키를 `vercel.app`의 요청에 포함시키지 않는다. Safari ITP는 이를 완전히 차단하고, Chrome도 Privacy Sandbox 정책으로 강화하는 추세다.

**현재 구조**: 서버가 JWT를 `HttpOnly` 쿠키와 응답 body 두 곳에 모두 반환하고, 프론트는 body의 token을 `localStorage`에 저장해 `Authorization: Bearer` 헤더로 전송한다. localStorage에 저장된 JWT는 XSS 공격으로 탈취될 수 있다.

**쿠키 전용으로 전환하려면 — Vercel Rewrite 프록시**

`vercel.json`에 rewrites를 추가해 프론트와 백엔드를 같은 출처처럼 만든다.

```json
{
  "rewrites": [
    { "source": "/api/:path*", "destination": "https://something.railway.app/api/:path*" }
  ]
}
```

프론트(`vercel.app/api/...`) 요청을 Vercel Edge가 백엔드로 프록시하면 쿠키가 같은 출처(`vercel.app`) 쿠키로 동작한다. 이 경우 `VITE_API_BASE_URL=/api`로 변경하고 `SecurityConfig` CORS 설정을 단순화해야 한다.
