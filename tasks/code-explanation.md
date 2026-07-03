# Briefy 코드 설명서

각 파일의 역할과 핵심 로직을 코드 레벨로 설명한다.  
전체 흐름 개요는 `codebase-guide.md` 참고.

---

## 목차

1. [공통 인프라 (global/)](#1-공통-인프라-global)
2. [인증 (domain/user/)](#2-인증-domainuser)
3. [일정 (domain/schedule/)](#3-일정-domainschedule)
4. [프론트엔드 — 인증 흐름](#4-프론트엔드--인증-흐름)
5. [프론트엔드 — 캘린더 화면](#5-프론트엔드--캘린더-화면)
6. [프론트엔드 — 일정 모달](#6-프론트엔드--일정-모달)
7. [파일별 한 줄 요약](#7-파일별-한-줄-요약)

---

## 1. 공통 인프라 (global/)

### `ApiResponse.java` — 모든 응답의 봉투

```java
public record ApiResponse<T>(
    boolean success,
    @Nullable T data,
    @Nullable String message
) { ... }
```

- **모든 컨트롤러는 이 타입만 반환한다.** 성공이면 `success: true, data: 실제값`, 실패면 `success: false, message: 에러메시지`.
- Java `record`라서 생성자/getter가 자동 생성된다. 필드는 불변.
- `ApiResponse.ok()` — data 없는 성공 (204 등)
- `ApiResponse.ok(data)` — 일반 성공
- `ApiResponse.error(errorCode)` — `BriefyErrorCode`의 message를 꺼내서 담음

---

### `BriefyErrorCode.java` — 에러 코드 카탈로그

```java
SCHEDULE_NOT_FOUND("S001", "일정을 찾을 수 없습니다"),
USER_ALREADY_EXISTS("U002", "이미 존재하는 사용자입니다"),
INVALID_CREDENTIALS("A004", "이메일 또는 비밀번호가 올바르지 않습니다"),
```

- enum 각 항목이 `(코드, 한국어 메시지)` 쌍을 갖는다.
- 새 에러를 추가할 때는 여기에 항목을 추가하고, `GlobalExceptionHandler`에서 HTTP 상태코드를 매핑하면 된다.

---

### `BriefyException.java` — 도메인 예외

```java
public class BriefyException extends RuntimeException {
    private final BriefyErrorCode errorCode;
    ...
}
```

- `RuntimeException`을 상속했으므로 checked exception 선언 없이 어디서든 `throw new BriefyException(BriefyErrorCode.XXX)` 할 수 있다.
- 예외 메시지는 `errorCode.getMessage()`를 그대로 쓴다.
- 서비스 레이어에서 던지면 `GlobalExceptionHandler`가 받아서 HTTP 응답으로 변환한다.

---

### `GlobalExceptionHandler.java` — 예외 → HTTP 응답 변환

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BriefyException.class)
    public ResponseEntity<ApiResponse<?>> handleBriefyException(BriefyException ex) {
        HttpStatus status = switch (ex.getErrorCode()) {
            case SCHEDULE_NOT_FOUND, USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FORBIDDEN                          -> HttpStatus.FORBIDDEN;
            case USER_ALREADY_EXISTS, SCHEDULE_OVERLAP -> HttpStatus.CONFLICT;
            ...
        };
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getErrorCode()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<?>> handleDataIntegrity(...) {
        if (msg.contains("schedules_no_overlap")) {
            // DB GiST EXCLUDE 제약 위반 → S002로 변환
            return ... ApiResponse.error(BriefyErrorCode.SCHEDULE_OVERLAP);
        }
        if (msg.contains("users_email_unique")) {
            // 동시 등록 레이스 컨디션: 서비스의 existsByEmail 체크 통과 후 DB 유니크 위반 → 409
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(BriefyErrorCode.USER_ALREADY_EXISTS));
        }
    }
}
```

- `@RestControllerAdvice` — 모든 컨트롤러에 적용되는 전역 예외 처리기.
- `BriefyException` 처리: errorCode별로 HTTP 상태를 매핑 (404/403/401/409/400).
- `DataIntegrityViolationException` 처리: JPA가 DB 제약 위반을 이 예외로 감싸서 던진다. `schedules_no_overlap`이면 S002, `users_email_unique`이면 U002로 변환. 이렇게 하지 않으면 DB 오류가 그대로 500으로 노출된다.
- `users_email_unique` 처리 이유: `AuthService.register()`가 `existsByEmail()`로 먼저 검사하지만, 동시 요청이 동시에 통과하면 DB 유니크 제약이 한 요청을 막는다. 이 경우를 `DataIntegrityViolationException`으로 잡아 의미 있는 오류로 변환한다.
- `MethodArgumentNotValidException`: `@Valid` 검증 실패 시 발생. 필드별 메시지를 `, `로 합쳐 반환.

---

### `JwtProvider.java` — JWT 생성·파싱

```java
// 생성
public String generate(UUID userId, String email) {
    return Jwts.builder()
        .subject(userId.toString())  // sub 클레임에 userId
        .claim("email", email)
        .expiration(new Date(System.currentTimeMillis() + expirationMs))
        .signWith(secretKey)
        .compact();
}

// 안전한 파싱 (실패해도 예외 안 던짐)
public Optional<Claims> tryParse(String token) {
    try { return Optional.of(parse(token)); }
    catch (JwtException | IllegalArgumentException e) { return Optional.empty(); }
}
```

- 시크릿 키는 `application.yml`의 `jwt.secret` 문자열을 **SHA-256으로 해시**해서 만든다. 짧은 문자열도 안전하게 256비트 키로 변환된다.
- `parse()`: 검증+파싱, 실패 시 예외.
- `tryParse()`: 필터에서 사용. 토큰이 이상해도 예외 없이 `Optional.empty()` 반환.
- `isValid()`: `tryParse().isPresent()` 래퍼.

---

### `JwtAuthenticationFilter.java` — 모든 요청에서 JWT 검증

```java
// 토큰 추출 우선순위
private String extractToken(HttpServletRequest request) {
    // 1순위: Authorization: Bearer <token>
    String authHeader = request.getHeader("Authorization");
    if (authHeader != null && authHeader.startsWith("Bearer "))
        return authHeader.substring(7);
    // 2순위: jwt 쿠키
    return Arrays.stream(request.getCookies())
        .filter(c -> "jwt".equals(c.getName()))
        .findFirst().map(Cookie::getValue).orElse(null);
}
```

- `OncePerRequestFilter`를 상속하므로 요청당 정확히 한 번 실행된다.
- 토큰을 성공적으로 파싱하면 `UserPrincipal`을 만들어 `SecurityContextHolder`에 저장한다. 이후 컨트롤러에서 `@AuthenticationPrincipal UserPrincipal principal`로 꺼낸다.
- 토큰이 없거나 유효하지 않으면 아무것도 하지 않고 다음 필터로 넘긴다. (인증 실패가 아니라 "미인증 상태"로 넘어감 → 이후 `SecurityConfig`의 규칙이 401 반환)

---

### `UserPrincipal.java` — SecurityContext 내 인증 주체

```java
// 두 가지 생성 경로
UserPrincipal.from(User user)      // DB에서 사용자를 조회한 뒤 (현재 미사용)
UserPrincipal.fromJwt(userId, email) // JWT 클레임에서 (필터에서 사용)
```

- Spring Security의 `UserDetails`를 구현. `getUsername()`은 email 반환.
- `getUserId()` 메서드가 핵심 — 컨트롤러에서 `principal.getUserId()`로 현재 사용자 ID를 가져온다.
- 권한은 고정으로 `ROLE_USER` 하나만.
- 비밀번호(`getPassword()`)는 JWT 경로에서는 null — 인증 과정에서 쓰지 않으므로 문제없다.

---

### `SecurityConfig.java` — Spring Security 설정

```java
http
    .csrf(disabled)                       // REST API이므로 CSRF 불필요
    .sessionManagement(STATELESS)         // 세션 미사용 (JWT 기반)
    .authorizeHttpRequests(auth -> auth
        .requestMatchers(OPTIONS, "/**").permitAll()   // CORS preflight
        .requestMatchers("/api/v1/auth/login", ...).permitAll()
        .anyRequest().authenticated()     // 나머지는 모두 JWT 필요
    )
    .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
```

- CORS 설정: `frontendUrl` (환경변수 `FRONTEND_URL`)과 `localhost:*`를 허용. `allowCredentials(true)`가 있으므로 `allowedOrigins` 대신 `allowedOriginPatterns`를 써야 한다 (Spring 제약).
- actuator 허용 목록: `/actuator/health`와 `/actuator/info`만 명시적으로 허용한다. `/actuator/env`(환경변수), `/actuator/heapdump`(힙덤프) 등 민감한 엔드포인트가 인증 없이 노출되지 않도록 필요한 것만 열어둔다.
- 인증 실패 시 `HttpStatusEntryPoint(401)` — 기본 동작(302 리다이렉트)을 막고 JSON API에 적합한 401을 반환.

---

### `NotificationScheduler.java` — 5분 주기 알림 스케줄러

```java
@Scheduled(fixedDelay = 300_000) // 5분마다
public void checkUpcomingSchedules() {
    OffsetDateTime now = OffsetDateTime.now();
    var upcoming = scheduleRepository.findUpcoming(now, now.plusMinutes(30), PageRequest.of(0, 200));
    if (!upcoming.isEmpty()) log.info("[알림] 30분 내 시작 일정 {}건", upcoming.size());
}
```

- 현재는 로그 출력만 한다. 실제 푸시/이메일 알림은 미구현 상태.
- `findUpcoming`: `rrule IS NULL`인 비반복 일정만 체크. 반복 일정은 현재 미지원.
- 200건 페이징 — 한 번에 너무 많은 일정을 메모리에 올리지 않도록.

---

## 2. 인증 (domain/user/)

### `User.java` — 사용자 엔티티

```java
@Entity
@Table(name = "users")
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)  // UUIDv7
    private UUID id;

    protected User() { }  // JPA 전용, 외부 직접 호출 금지

    public User(@NonNull String email, @NonNull String name) { ... }  // 유일한 공개 생성자

    // 도메인 메서드 — 세터 대신 의도를 담은 메서드
    public void updateName(String name) { this.name = name; }
    public void updatePasswordHash(String hash) { this.passwordHash = hash; }
}
```

- `protected User()`: JPA가 리플렉션으로 객체를 복원할 때 필요하다. `protected`이므로 외부에서 빈 객체를 만들지 못한다.
- `@UuidGenerator(style = TIME)`: Hibernate 6가 UUIDv7을 생성한다. 시간 순서가 담겨 있어 B-tree 인덱스 단편화가 줄어든다.
- `@EntityListeners(AuditingEntityListener.class)` + `@CreatedDate` / `@LastModifiedDate`: Hibernate가 저장/수정 시 자동으로 타임스탬프를 채운다.
- `passwordHash`는 `@Nullable` — OAuth 로그인 사용자는 비밀번호가 없다.

---

### `AuthService.java` — 회원가입 / 로그인

```java
@Transactional
public AuthResult register(String email, String name, String password) {
    if (userRepository.existsByEmail(email))
        throw new BriefyException(BriefyErrorCode.USER_ALREADY_EXISTS);

    User user = new User(email, name);
    user.updatePasswordHash(passwordEncoder.encode(password));  // BCrypt 해싱
    user = userRepository.save(user);
    return new AuthResult(user, jwtProvider.generate(user.getId(), user.getEmail()));
}

public AuthResult login(String email, String password) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BriefyException(BriefyErrorCode.INVALID_CREDENTIALS));
    // 이메일 없음과 비밀번호 불일치를 동일한 에러로 처리 → 계정 존재 여부 노출 방지
    if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash()))
        throw new BriefyException(BriefyErrorCode.INVALID_CREDENTIALS);
    return new AuthResult(user, jwtProvider.generate(...));
}
```

- `AuthResult`: `{ User user, String token }` record. 컨트롤러가 이걸 받아서 쿠키 설정 + 응답 반환.
- 비밀번호 비교: `passwordEncoder.matches(raw, hash)` — BCrypt가 내부적으로 솔트를 처리한다. 단순 `equals`로 비교하면 안 된다.
- 보안 원칙: 이메일 없음과 비밀번호 불일치 모두 `INVALID_CREDENTIALS`(A004)로 반환. 공격자가 어떤 이메일이 등록됐는지 알지 못하게.

---

### `AuthController.java` — 인증 API

```java
@PostMapping("/login")
public ApiResponse<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                       HttpServletResponse response) {
    AuthResult result = authService.login(request.email(), request.password());
    setJwtCookie(response, result.token());  // HttpOnly 쿠키 설정
    return ApiResponse.ok(new AuthResponse(UserResponse.from(result.user()), result.token()));
    //                                                                        ↑ 토큰을 body에도 포함
}

private void setJwtCookie(HttpServletResponse response, String token) {
    response.addHeader(HttpHeaders.SET_COOKIE,
        ResponseCookie.from("jwt", token)
            .httpOnly(true)     // JS에서 document.cookie로 접근 불가 (XSS 방어)
            .path("/")
            .maxAge(Duration.ofDays(7))
            .build().toString());
}
```

- JWT를 **쿠키와 body 두 곳에 모두** 반환한다. 프론트는 body의 token을 localStorage에 저장하고, 이후 요청에 `Authorization: Bearer` 헤더로 보낸다. 쿠키는 브라우저가 자동으로 포함시킨다.
- `httpOnly(true)`: JavaScript로 쿠키를 읽을 수 없다 → XSS 공격으로 토큰 탈취를 막는다.
- `@GetMapping("/me")`: 이미 인증된 사용자의 정보 조회. 앱 시작 시 `AuthContext`가 이걸 호출해 세션 복원.

---

### `UserService.java` — 사용자 CRUD

```java
public User findById(UUID userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new BriefyException(BriefyErrorCode.USER_NOT_FOUND));
}

@Transactional
public User updateName(UUID userId, String name) {
    User user = findById(userId);
    user.updateName(name);  // dirty checking — save() 호출 없이 트랜잭션 끝에 자동 UPDATE
    return user;
}
```

- `updateName`에서 `userRepository.save()`를 명시적으로 호출하지 않는다. JPA의 **변경 감지(dirty checking)** 덕분에 `@Transactional` 메서드가 끝날 때 변경된 필드를 자동으로 UPDATE한다.
- `ScheduleService`도 같은 패턴으로 `update()` 구현.
- `delete`: `CASCADE DELETE`가 설정돼 있어서 사용자 삭제 시 해당 사용자의 모든 일정이 DB에서 자동 삭제된다.

---

## 3. 일정 (domain/schedule/)

### `Schedule.java` — 일정 엔티티

```java
public Schedule(User user, String title, ..., String rrule, boolean skipHolidays, String color) {
    if (!endTime.isAfter(startTime))
        throw new BriefyException(BriefyErrorCode.SCHEDULE_INVALID_TIME);
    // 유효성 검사를 생성자에서 수행 — 잘못된 상태의 객체가 존재할 수 없음
    ...
}

// rrule 필드: null이면 비반복, 값이 있으면 반복 일정
@Nullable
private String rrule;  // 예: "FREQ=WEEKLY;BYDAY=MO,WE,FR"
```

- 생성자에서 `endTime > startTime`을 강제한다. 서비스나 컨트롤러에서 검사하지 않아도 된다.
- `rrule`이 핵심 구분자다. null이면 단순 일정, 값이 있으면 반복 일정.
- DB에도 `CHECK (end_time > start_time)` 제약이 있어 이중으로 막는다.

---

### `ScheduleRepository.java` — 쿼리 모음

**비반복 일정 범위 조회**
```java
@Query("""
    SELECT s FROM Schedule s
    WHERE s.user.id = :userId
    AND s.rrule IS NULL             -- 비반복만
    AND s.startTime < :rangeEnd     -- 범위 안에서 시작하거나
    AND s.endTime > :rangeStart     -- 범위 안에서 끝나는 것
    ORDER BY s.startTime
""")
List<Schedule> findNonRecurringByUserAndRange(...);
```
시간 범위 겹침 조건: `startTime < rangeEnd AND endTime > rangeStart`. 이 패턴이 "두 구간이 겹치는 모든 경우"를 정확히 표현한다.

**반복 일정 조회**
```java
@Query("""
    SELECT s FROM Schedule s
    WHERE s.user.id = :userId
    AND s.rrule IS NOT NULL
    AND s.startTime < :rangeEnd    -- 발생이 rangeEnd 이전에 시작했어야 함
""")
List<Schedule> findRecurringByUser(...);
```
반복 일정은 DB 쿼리에서 정확한 발생일을 계산할 수 없어서, 조회 후 서비스에서 RRULE을 직접 확장한다.

**소유권 확인 쿼리**
```java
@Query("SELECT s FROM Schedule s JOIN FETCH s.user WHERE s.id = :id AND s.user.id = :userId")
Optional<Schedule> findByIdAndUserId(UUID id, UUID userId);
```
`JOIN FETCH s.user`: 일정과 사용자를 한 쿼리로 가져온다. 없으면 N+1 문제 — 일정 조회 후 user 접근 시 추가 쿼리가 발생한다.

---

### `ScheduleService.java` — 핵심 비즈니스 로직

**일정 목록 조회 (반복 확장 포함)**
```java
public List<ScheduleEventResponse> listEvents(UUID userId, OffsetDateTime from, OffsetDateTime to) {
    List<ScheduleEventResponse> events = new ArrayList<>();

    // 1단계: 비반복 일정을 그대로 추가
    scheduleRepository.findNonRecurringByUserAndRange(userId, from, to)
        .forEach(s -> events.add(ScheduleEventResponse.from(s)));

    // 2단계: 반복 일정 확장
    scheduleRepository.findRecurringByUser(userId, to).forEach(s -> {
        Duration duration = Duration.between(s.getStartTime(), s.getEndTime());  // 원본 길이 보존
        expandRrule(s.getRrule(), s.getStartTime(), from, to, s.isSkipHolidays())
            .forEach(occ -> events.add(ScheduleEventResponse.occurrence(s, occ, occ.plus(duration))));
    });

    events.sort(Comparator.comparing(ScheduleEventResponse::startTime));
    return events;
}
```

**RRULE 확장 (expandRrule)**
```java
private List<OffsetDateTime> expandRrule(String rrule, OffsetDateTime dtStart, ...) {
    // biweekly 라이브러리가 iCalendar 형식을 기대하므로 임시 .ics 문자열 조립
    String ics = "BEGIN:VCALENDAR\r\nVERSION:2.0\r\n" +
                 "BEGIN:VEVENT\r\n" +
                 "DTSTART:" + dtStartStr + "\r\n" +
                 "RRULE:" + rrule + "\r\n" +
                 "END:VEVENT\r\n" +
                 "END:VCALENDAR";

    ICalendar ical = Biweekly.parse(ics).first();
    var it = ical.getEvents().get(0).getDateIterator(TimeZone.getTimeZone("UTC"));

    // 이터레이터로 발생일을 하나씩 꺼내 범위 안에 드는 것만 수집
    while (it.hasNext() && occurrences.size() < 500) {
        OffsetDateTime odt = it.next().toInstant().atOffset(ZoneOffset.UTC);
        if (odt.isAfter(rangeEnd)) break;    // 범위 초과 → 중단
        if (odt.isBefore(rangeStart)) continue; // 범위 미만 → 건너뜀
        if (skipHolidays && KoreanHolidays.isHoliday(...)) continue;
        occurrences.add(odt);
    }
}
```

- `biweekly` 라이브러리는 단독 RRULE 문자열을 처리하지 못하고 완전한 `.ics` 파일 형식이 필요하다. 그래서 임시로 VCALENDAR 문자열을 조립한다.
- 최대 500개 발생(RRULE_OCCURRENCE_LIMIT)으로 무한 반복 방지.
- `skipHolidays`: `KoreanHolidays.isHoliday()`로 공휴일/대체휴일 여부 판단 후 제외.

**소유권 검증**
```java
private Schedule findOwnedSchedule(UUID userId, UUID scheduleId) {
    return scheduleRepository.findByIdAndUserId(scheduleId, userId)
            .orElseThrow(() -> new BriefyException(BriefyErrorCode.SCHEDULE_NOT_FOUND));
}
```
"없는 일정"과 "남의 일정" 둘 다 동일하게 404(`SCHEDULE_NOT_FOUND`)를 반환한다. 403과 404를 구분하면 IDOR(Insecure Direct Object Reference) 열거 취약점이 생긴다 — 공격자가 ID를 바꾸면서 "403이면 존재", "404면 없음"으로 다른 사용자의 일정 목록을 열거할 수 있기 때문이다. 어떤 경우든 404로 통일해 일정 존재 여부를 외부에 노출하지 않는다.

---

### `ScheduleEventResponse.java` vs `ScheduleResponse.java`

두 DTO의 차이:

| | `ScheduleResponse` | `ScheduleEventResponse` |
|---|---|---|
| 용도 | 단일 일정 CRUD 응답 | 캘린더 범위 조회 응답 |
| startTime/endTime | 원본값 | 반복이면 **확장된 발생일** |
| `recurring` 필드 | 없음 | 있음 (반복 발생이면 `true`) |
| `skipHolidays` 필드 | 있음 | 있음 |

`ScheduleEventResponse`가 `skipHolidays`를 포함하는 이유: `CalendarPage.openEdit(ev)`는 `getOne()` API 호출이 실패하면 캘린더에 있는 `ev`(ScheduleEventResponse)를 직접 모달에 전달하는 폴백 경로가 있다. 이 경로에서도 `skipHolidays` 값이 정확히 전달돼야 모달 초기값이 올바르게 채워진다.

반복 일정의 경우 DB에는 원본 행 1개지만, 캘린더에는 발생 횟수만큼 `ScheduleEventResponse`가 만들어진다. 모두 같은 `id`를 가지므로, 프론트에서 수정/삭제 클릭 시 `getOne(id)`로 원본 `ScheduleResponse`를 다시 조회한다.

---

## 4. 프론트엔드 — 인증 흐름

### `client.js` — fetch 래퍼

```js
async function request(path, options = {}) {
    const token = getToken()  // localStorage.getItem('jwt')
    const res = await fetch(`${BASE_URL}${path}`, {
        credentials: 'include',  // 쿠키 자동 포함
        headers: {
            ...(hasBody && { 'Content-Type': 'application/json' }),
            ...(token && { 'Authorization': `Bearer ${token}` }),
        },
        ...rest,
    })
    if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        // err.message: 백엔드 ApiResponse의 message 필드
        throw Object.assign(new Error(err.message ?? res.statusText), { status: res.status, body: err })
    }
    if (res.status === 204) return null
    return res.json()
}
```

- `credentials: 'include'`: 쿠키를 cross-origin 요청에도 포함. CORS 설정에서 `allowCredentials(true)`와 쌍을 이룬다.
- 에러 시 `err.body`에 서버 응답 전체를 담아 throw. 폼에서 `err.body?.message`로 한국어 에러 메시지를 꺼낸다.
- 204(No Content)는 body가 없으므로 `res.json()`을 호출하지 않고 null 반환.

---

### `AuthContext.jsx` — 인증 상태 전역 관리

```jsx
export function AuthProvider({ children }) {
    const [user, setUser] = useState(null)
    const [loading, setLoading] = useState(true)  // 초기화 중 여부

    // 앱 시작 시 세션 복원
    useEffect(() => { checkAuth() }, [checkAuth])

    const login = useCallback(async (email, password) => {
        const res = await authApi.login({ email, password })
        setToken(res.data.token)       // localStorage 저장
        setUser(res.data.user)         // Context 상태 업데이트
        saveAccount({ email: ..., name: ... })  // 최근 로그인 목록 저장
        return res.data.user
    }, [])

    const logout = useCallback(async () => {
        setToken(null)          // localStorage 제거 (즉시)
        await authApi.logout()  // 쿠키 만료 처리 (서버)
        setUser(null)           // Context 상태 초기화
    }, [])
    const deleteAccount = useCallback(async () => {
        await usersApi.deleteMe()
        setToken(null)                           // 즉시 상태 정리
        setUser(null)
        await authApi.logout().catch(() => {})   // 쿠키 만료 — 실패해도 괜찮음
    }, [])
    ...
}
```

- `useCallback`: 함수 참조가 매 렌더마다 바뀌지 않도록 메모이제이션. `useEffect` 의존성 배열에 함수를 넣을 때 무한 루프를 방지.
- `loading`: `checkAuth()`가 완료되기 전에는 true. `ProtectedRoute`가 이 값을 보고 리다이렉트 여부를 결정한다.
- Context value로 `{ user, loading, login, register, logout, updateUser, deleteAccount }` 전체를 노출.
- `deleteAccount` 순서 이유: `setToken/setUser`를 `authApi.logout()` **앞에** 실행한다. logout API가 실패해도 클라이언트 상태는 이미 정리된 상태이므로 사용자가 로그아웃 상태로 돌아온다. logout은 쿠키 만료용이라 실패해도 기능적으로 문제없다.

---

### `ProtectedRoute.jsx` — 인증 가드

```jsx
export default function ProtectedRoute({ children }) {
    const { user, loading } = useAuth()
    if (loading) return <div className="app-loading">로딩 중…</div>
    if (!user) return <Navigate to="/login" replace />
    return children
}
```

세 가지 상태:
1. `loading === true`: 아직 `/auth/me` 응답 안 옴 → 스피너 표시
2. `loading === false && user === null`: 미인증 → `/login` 리다이렉트
3. `loading === false && user !== null`: 인증됨 → 자식 컴포넌트 렌더

`replace` prop: 히스토리 스택에 `/login`을 push하지 않고 replace해서, 로그인 후 뒤로가기 했을 때 다시 리다이렉트 루프에 빠지지 않는다.

---

### `LoginPage.jsx` — 계정 목록 + 로그인 폼

두 가지 화면을 하나의 컴포넌트에서 처리한다.

**상태 머신 (showForm)**
```
저장된 계정 있음 → 계정 목록 화면 (showForm = false)
    → 계정 선택 → showForm = true, 이메일 자동 채워짐
    → "다른 계정" 클릭 → showForm = true, 이메일 비어있음
저장된 계정 없음 → 바로 폼 화면 (showForm = true)
```

- `savedAccounts.js`: localStorage의 `briefy_accounts` 키에 최근 5개 계정(email, name)을 저장. 로그인 성공 시 `saveAccount()`로 업데이트.
- 계정 선택 시 이메일 입력을 숨기고 계정 카드를 보여준다 (`selectedAccount` 상태).

---

## 5. 프론트엔드 — 캘린더 화면

### `CalendarPage.jsx` — 상태 관리 허브

이 컴포넌트가 캘린더 화면의 모든 상태를 관리한다.

**주요 상태**
```js
const [year, month]        // 현재 보고 있는 연·월
const [events, setEvents]  // 해당 월의 일정 목록
const [selectedDate]       // 클릭된 날짜 (하단 패널)
const [modal]              // { open: bool, schedule: null | 객체 }
const [searchQuery]        // 검색어
const deferredQuery        // useDeferredValue로 타이핑 딜레이
```

**fetchEvents 날짜 경계**
```js
const from = new Date(y, m, 1).toISOString()      // 로컬 타임 기준 월 시작
const to   = new Date(y, m + 1, 1).toISOString()  // 로컬 타임 기준 다음 달 시작
```
`fieldsToISO()`가 날짜를 로컬 타임 기준으로 변환하므로, 조회 범위도 같은 기준을 써야 한다. 예를 들어 KST(UTC+9)에서 6월 1일 오전 9시에 생성된 일정은 UTC로 전날(5월 31일) 자정이 된다 — UTC 기준으로 범위를 잡으면 6월 이벤트가 범위 밖으로 빠진다.

**월 변경 시 자동 재조회**
```js
useEffect(() => {
    const controller = new AbortController()
    fetchEvents(year, month, controller.signal)
    return () => controller.abort()  // 빠른 월 전환 시 이전 요청 취소
}, [year, month, fetchEvents])
```
`AbortController`: 연속으로 월을 바꾸면 이전 fetch를 취소해 응답이 엇갈리는(race condition) 문제를 방지한다.

**검색**
```js
const deferredQuery = useDeferredValue(searchQuery)
useEffect(() => {
    const q = deferredQuery.trim()
    if (!q) { setSearchResults([]); return }
    // 검색어 바뀔 때마다 API 호출
}, [deferredQuery])
```
`useDeferredValue`: React 18의 동시성 기능. 타이핑 중에는 렌더링을 잠깐 미루어 UI가 버벅이지 않게 한다.

**편집 시 원본 조회**
```js
async function openEdit(ev) {
    const res = await schedulesApi.getOne(ev.id)  // 원본 ScheduleResponse 조회
    setModal({ open: true, schedule: res.data })
}
```
캘린더에 표시된 `ev`는 `ScheduleEventResponse` (반복 일정이면 확장된 발생일). 수정하려면 원본 startTime/endTime/rrule이 필요하므로 `getOne`으로 다시 조회.

---

### `MonthCalendar.jsx` — 월간 그리드

**그리드 생성 로직**
```js
function getMonthGrid(year, month) {
    const firstDay = new Date(year, month, 1)
    const cells = []
    // 1일의 요일만큼 앞에 빈 셀 채우기 (일요일 기준)
    for (let i = 0; i < firstDay.getDay(); i++) cells.push(null)
    // 날짜 채우기
    for (let d = 1; d <= lastDay.getDate(); d++) cells.push(new Date(year, month, d))
    return cells
}
```

**이벤트 맵 (O(1) 날짜별 조회)**
```js
const eventMap = useMemo(() => {
    const map = {}
    events.forEach(ev => {
        const key = `${year}-${month}-${date}`  // "2026-5-15"
        if (!map[key]) map[key] = []
        map[key].push(ev)
    })
    return map
}, [events])
```
매 셀마다 `events.filter()`를 하면 O(n×m). 대신 한 번 맵을 만들어두면 셀 렌더링이 O(1).

**이벤트 표시**: 날짜 셀에 최대 3개 표시, 초과 시 `+n` 텍스트.

---

## 6. 프론트엔드 — 일정 모달

### `ScheduleModal.jsx` — 시간 입력 구조

가장 복잡한 컴포넌트. 시간 입력을 커스텀으로 구현했다.

**폼 상태 구조**
```js
{
    title, description, color,
    startDate,   // "2026-06-30"
    startHour,   // "9" (12시간제, 문자열)
    startMinute, // "00"
    startAmPm,   // "오전" | "오후"
    endDate, endHour, endMinute, endAmPm,
    rrule, skipHolidays
}
```

**12시간 ↔ 24시간 변환**
```js
function to12h(h24) {
    if (h24 === 0) return { hour: '0', ampm: '오전' }
    if (h24 < 12) return { hour: String(h24), ampm: '오전' }
    if (h24 === 12) return { hour: '12', ampm: '오후' }
    return { hour: String(h24 - 12), ampm: '오후' }
}

function to24h(hour12, ampm) {
    const h = parseInt(hour12, 10)
    if (isNaN(h) || h > 24) return 0   // 범위 초과·NaN → 0으로 안전 처리
    if (h >= 13 && h <= 23) return h   // 13~23 입력은 24시간제로 직접 취급
    if (h === 0 || h === 24) return 0  // 0과 24는 자정
    if (ampm === '오전') return h === 12 ? 0 : h
    return h === 12 ? 12 : h + 12     // 오후
}
```

`isNaN(h) || h > 24` 조건이 필요한 이유: `fieldsToISO`는 내부에서 `new Date('2026-06-30T25:00')` 같은 문자열을 만들 수 있는데, 브라우저에 따라 이를 다음 날로 롤오버하거나 Invalid Date로 처리한다. 범위를 벗어난 값은 미리 0으로 클램프해 예측 불가능한 동작을 막는다.

**시간 blur 처리 (스마트 자동 보정)**
```js
function handleHourBlur(prefix) {
    return () => {
        setForm(prev => {
            const h = parseInt(prev[`${prefix}Hour`], 10)
            if (isNaN(h)) return { ...prev, [`${prefix}Hour`]: '0' }
            if (h === 0) return { ...prev, [`${prefix}Hour`]: '0', [`${prefix}AmPm`]: '오전' }
            if (h >= 13 && h <= 23) {
                // 14 입력 → 오후 2시로 자동 전환
                return { ...prev, [`${prefix}Hour`]: String(h - 12), [`${prefix}AmPm`]: '오후' }
            }
            if (h === 24) {
                // 24 입력 → 날짜를 하루 앞당기고 오전 0시로
                const d = new Date(prev[`${prefix}Date`] + 'T00:00:00')
                d.setDate(d.getDate() + 1)
                const pad = n => String(n).padStart(2, '0')
                const newDate = `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
                return { ...prev, [`${prefix}Hour`]: '0', [`${prefix}AmPm`]: '오전', [`${prefix}Date`]: newDate }
            }
            if (h > 24) return { ...prev, [`${prefix}Hour`]: '12' }
            return prev
        })
    }
}
```

`h === 0` 브랜치에서 `AmPm`을 `'오전'`으로 명시적으로 설정한다. 자정(0시)은 오전/오후 구분 없이 항상 오전에 해당하므로, 포커스가 빠질 때 기존 AmPm 상태와 무관하게 오전으로 고정한다.

**분 blur 처리**
```js
function handleMinuteBlur(prefix) {
    return () => {
        setForm(prev => {
            const m = parseInt(prev[`${prefix}Minute`], 10)  // prev에서 읽어야 함
            const clamped = isNaN(m) ? 0 : Math.min(59, m)
            return { ...prev, [`${prefix}Minute`]: String(clamped).padStart(2, '0') }
        })
    }
}
```

`setForm(prev => ...)` 형태의 함수형 updater 안에서 `prev[...]`로 분 값을 읽는다. `handleMinuteBlur`는 `prefix`를 캡처한 클로저이므로, 만약 `form[...]`을 직접 읽으면 함수가 생성된 시점의 오래된 상태를 참조하는 stale closure 문제가 생긴다. updater 함수의 `prev` 인자는 항상 React가 보장하는 최신 상태를 받는다.

**숨겨진 datetime-local 피커**
```jsx
<input ref={startDtRef} type="datetime-local" className="hidden-dt-input"
       value={dtLocalValue('start')} onChange={handleDtLocal('start')} tabIndex={-1} />
<button onClick={() => startDtRef.current?.showPicker()}>
    <CalendarIcon />
</button>
```
`input[type="datetime-local"]`을 숨기고 아이콘 버튼으로 피커를 열면, 브라우저 기본 UI를 재활용하면서 커스텀 시각화를 유지할 수 있다. 선택 값은 `handleDtLocal()`이 받아서 분해해 각 필드에 반영.

**제출 시 ISO 변환**
```js
await onSave({
    startTime: fieldsToISO(form.startDate, form.startHour, form.startMinute, form.startAmPm),
    // → "2026-06-30T09:00:00.000Z"
})
```

---

### `SettingsPage.jsx` — 계정 관리

**계정 삭제 단계 상태 머신**
```
'idle'     → "계정 삭제" 버튼 클릭 → 'confirm'
'confirm'  → "삭제 확인" 클릭    → 'deleting'
'deleting' → API 성공           → navigate('/login')
'deleting' → API 실패           → 'confirm' + 에러 메시지
```

중간 확인 단계를 두어 실수로 계정을 삭제하는 것을 방지한다.

---

### `ScheduleServiceTest.java` — 서비스 단위 테스트

```java
@Test
void getOne_otherUsersSchedule_throwsNotFound() {
    when(scheduleRepository.findByIdAndUserId(scheduleId, userId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> scheduleService.getOne(userId, scheduleId))
        .isInstanceOf(BriefyException.class)
        .satisfies(e -> assertThat(((BriefyException) e).getErrorCode())
            .isEqualTo(BriefyErrorCode.SCHEDULE_NOT_FOUND));
}
```

- `findByIdAndUserId`가 empty를 반환하는 것만 설정하면 된다. `findOwnedSchedule`가 추가 쿼리 없이 바로 `SCHEDULE_NOT_FOUND`를 던지기 때문이다. 다른 사용자의 일정 ID를 넘겨도, 존재하지 않는 ID를 넘겨도 이 케이스에서 동일하게 처리된다.
- `@ExtendWith(MockitoExtension.class)`는 기본적으로 `STRICT_STUBS` 모드다. stub을 설정했는데 테스트 중 실제로 호출되지 않으면 `UnnecessaryStubbingException`으로 실패한다. mock을 만들 때는 실제로 호출되는 것만 stub해야 한다.

---

## 7. 파일별 한 줄 요약

### 백엔드

| 파일 | 역할 |
|---|---|
| `BriefyApplication.java` | Spring Boot 진입점, `@EnableJpaAuditing` |
| `ApiResponse.java` | `{ success, data, message }` 공통 응답 레코드 |
| `BriefyErrorCode.java` | 에러 코드 enum (S001/U001/A001 …) |
| `BriefyException.java` | 도메인 예외 (RuntimeException) |
| `GlobalExceptionHandler.java` | 예외 → HTTP 상태 + ApiResponse 변환 |
| `JwtProvider.java` | JWT 생성(generate), 파싱(parse/tryParse) |
| `JwtAuthenticationFilter.java` | 요청마다 JWT 추출 → SecurityContext 저장 |
| `JwtProperties.java` | `jwt.secret`, `jwt.expiration-ms` 바인딩 |
| `CookieProperties.java` | `cookie.secure`, `cookie.same-site` 바인딩 |
| `UserPrincipal.java` | SecurityContext에 저장되는 인증 주체 |
| `SecurityConfig.java` | 필터 체인, CORS, 공개 엔드포인트 목록 |
| `NotificationScheduler.java` | 5분 주기로 30분 내 일정 로그 (알림 미구현) |
| `KoreanHolidays.java` | 한국 공휴일 판단 유틸 |
| `User.java` | 사용자 JPA 엔티티 |
| `UserRepository.java` | JPA 인터페이스 (`findByEmail`, `existsByEmail`) |
| `AuthService.java` | register/login — 비밀번호 해시, JWT 발급 |
| `UserService.java` | findById/updateName/delete |
| `PasswordResetService.java` | 비밀번호 재설정 (현재 비활성화) |
| `AuthController.java` | `/api/v1/auth/**` — login/register/logout/me |
| `UserController.java` | `/api/v1/users/me` — GET/PATCH/DELETE |
| `Schedule.java` | 일정 JPA 엔티티 (rrule, skipHolidays, color) |
| `ScheduleRepository.java` | 범위 조회, 반복 일정 조회, 검색, 알림용 쿼리 |
| `ScheduleService.java` | CRUD + RRULE 확장 + 소유권 검증 |
| `ScheduleController.java` | `/api/v1/schedules/**` — CRUD + 검색 |
| `ScheduleRequest.java` | 일정 생성/수정 요청 DTO (record, `@NotBlank` 등) |
| `ScheduleResponse.java` | 단일 일정 응답 DTO |
| `ScheduleEventResponse.java` | 범위 조회 응답 (반복 일정은 발생일마다 생성) |

### 프론트엔드

| 파일 | 역할 |
|---|---|
| `main.jsx` | `ReactDOM.createRoot` 진입점 |
| `App.jsx` | BrowserRouter + AuthProvider + 라우팅 |
| `AuthContext.jsx` | user, loading, login/logout/register/updateUser/deleteAccount |
| `ProtectedRoute.jsx` | loading 대기 → 미인증이면 `/login` 리다이렉트 |
| `client.js` | fetch 래퍼 (JWT 헤더 자동, 에러 변환) |
| `auth.js` | `authApi` — login/register/logout/me/forgotPassword |
| `schedules.js` | `schedulesApi` — list/getOne/create/update/delete/search |
| `users.js` | `usersApi` — updateMe/deleteMe |
| `CalendarPage.jsx` | 메인 화면 — 월 탐색, 검색, 날짜 패널, 이벤트 상태 |
| `MonthCalendar.jsx` | 월간 그리드 컴포넌트 (공휴일, 이벤트 점) |
| `ScheduleModal.jsx` | 일정 생성/수정 모달 (12시간 입력, RRULE 선택) |
| `YearMonthPicker.jsx` | 연·월 드롭다운 피커 |
| `LoginPage.jsx` | 저장된 계정 목록 + 로그인 폼 |
| `RegisterPage.jsx` | 회원가입 폼 |
| `SettingsPage.jsx` | 이름 수정, 계정 삭제 (삭제 2단계 확인) |
| `useDarkMode.js` | 다크모드 토글 훅 (localStorage + prefers-color-scheme) |
| `savedAccounts.js` | 최근 로그인 계정 목록 (localStorage, 최대 5개) |
| `holidays.js` | 프론트 공휴일 표시용 데이터 |
