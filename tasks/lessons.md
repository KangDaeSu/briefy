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

## PostgreSQL

### [2026-05-15] PG18 UUIDv7 함수명: uuid_generate_v7() 아님
**증상**: Flyway 마이그레이션 실패 — `ERROR: function uuid_generate_v7() does not exist`
**원인**: `uuid_generate_v7()`은 `pg_uuidv7` 확장의 함수명. PG18 네이티브 빌트인은 `uuidv7()`
**해결**: 모든 DDL에서 `DEFAULT uuid_generate_v7()` → `DEFAULT uuidv7()`로 교체
**교훈**: PG18 네이티브 UUIDv7은 `uuidv7()`. 확장 없이 쓸 수 있지만 이름이 다름. 문서 확인 필수

---

### [2026-05-15] PostgreSQL CHAR(n) vs VARCHAR(n) Hibernate 스키마 검증 실패
**증상**: `SchemaManagementException: wrong column type; found [bpchar], but expecting [varchar(64)]`
**원인**: SQL `CHAR(64)`는 PostgreSQL 내부적으로 `bpchar` 타입. Hibernate Java `String` 필드는 `varchar` 기대
**해결**: DDL을 `CHAR(64)` → `VARCHAR(64)`로 변경
**교훈**: Hibernate `ddl-auto: validate` 사용 시 `CHAR` 컬럼은 반드시 `VARCHAR`로 선언

---

### [2026-05-12] PostgreSQL 18 Docker 볼륨 경로 오류
**증상**: briefy-db 컨테이너가 계속 restarting 상태. "There appears to be PostgreSQL data in /var/lib/postgresql/data (unused mount/volume)" 에러
**원인**: PostgreSQL 18부터 PGDATA 기본 경로가 변경됨 (구버전: `/var/lib/postgresql/data` → 신버전: `/var/lib/postgresql/18/docker`)
**해결**: docker-compose.yml 볼륨 마운트를 `/var/lib/postgresql/data` → `/var/lib/postgresql`로 변경 후 `docker compose down -v` 재시작
**교훈**: PostgreSQL 18+ Docker 설정 시 볼륨은 `/var/lib/postgresql`로 마운트. 구버전 예제 그대로 복사하면 안 됨

---

## Spring Boot 4.0

### [2026-05-15] @MockBean 제거됨 → @MockitoBean 사용
**증상**: `package org.springframework.boot.test.mock.mockito does not exist` 컴파일 오류
**원인**: Spring Boot 4.0에서 `@MockBean`, `@SpyBean` 완전 제거
**해결**:
```java
// Before (Spring Boot 3.x 이하)
import org.springframework.boot.test.mock.mockito.MockBean;
@MockBean MyService myService;

// After (Spring Boot 4.0)
import org.springframework.test.context.bean.override.mockito.MockitoBean;
@MockitoBean MyService myService;
```
**교훈**: Spring Boot 4.0 마이그레이션 시 테스트 코드의 `@MockBean` 전수 교체 필요

---

### [2026-05-15] Testcontainers 2.x 아티팩트명 변경 및 JUnit5 확장 분리
**증상**: `package org.testcontainers.junit.jupiter does not exist` 컴파일 오류
**원인**: Testcontainers 2.x에서 모듈 이름 변경 + JUnit 5 확장이 별도 아티팩트로 분리
**해결**:
```xml
<!-- Before (1.x) -->
<artifactId>postgresql</artifactId>

<!-- After (2.x) -->
<artifactId>testcontainers-postgresql</artifactId>

<!-- 추가 필요 (2.x): @Testcontainers, @Container 어노테이션 -->
<artifactId>testcontainers-junit-jupiter</artifactId>
```
**교훈**: Testcontainers 2.x 업그레이드 시 아티팩트명 변경 + junit-jupiter 별도 추가 필수

---

## React 19

### [2026-06-02] 모바일 input[type="date"] 네이티브 최소 너비로 가로 오버플로 발생
**증상**: 모바일(393px)에서 ScheduleModal 날짜 입력 행이 뷰포트를 벗어나 가로 스크롤 발생. 로고와 버튼 일부가 잘림
**원인**: `input[type="date"]`는 브라우저가 네이티브 UI를 렌더링하기 위한 최소 너비를 강제함. `flex: 0 0 auto`와 조합 시 좁은 화면에서 flex 컨테이너를 초과
**해결**:
```css
@media (max-width: 640px) {
  .datetime-box { flex-wrap: wrap; }
  .datetime-box .date-input {
    flex: 1 0 100%;   /* 날짜 입력이 첫 행 전체를 차지 */
  }
}
```
**교훈**: 날짜·시간 복합 입력 UI는 모바일에서 `flex-wrap: wrap` 필수. `input[type="date"]` 네이티브 최소 너비를 flex 레이아웃 설계 시 반드시 고려할 것

---

## E2E / Playwright

### [2026-06-02] opacity:0 숨긴 input을 Playwright fill()로 채울 수 없음
**증상**: `fill()` 타임아웃 — `hidden-dt-input`에 값을 입력하려 했으나 Playwright 가시성 검사 실패
**원인**: Playwright `fill()`은 요소가 visible한지 확인. `opacity: 0; pointer-events: none` CSS가 적용된 native input은 interactable하지 않다고 판단해 거부
**해결**: 숨겨진 native input 대신 사용자가 실제로 보는 visible 필드(`.date-input`, `.time-hour-input`)를 직접 채움
**교훈**: 커스텀 날짜·시간 피커 테스트 시 숨겨진 native input이 아닌 visible 요소를 타겟으로 할 것. `force: true` 옵션은 회피책이지 해결책이 아님

---

### [2026-06-02] UI 리팩토링 후 E2E 셀렉터 미동기화 (셀렉터 드리프트)
**증상**: `page.locator('h1')` 타임아웃 — 연·월 표시가 `h1`에서 `button.cal-page__ym-btn`으로 변경됐는데 테스트가 업데이트되지 않음
**원인**: UI 컴포넌트 리팩토링 시 E2E 셀렉터를 동기화하지 않음. CI에서 E2E 단계가 없거나 통과 여부를 확인하지 않으면 드리프트가 누적됨
**해결**: 셀렉터를 실제 DOM 구조에 맞게 수정
**교훈**: UI 컴포넌트 변경 시 E2E 셀렉터를 항상 함께 수정. 시맨틱 셀렉터(role, label)를 우선하고 구조적 셀렉터(h1, div)는 최소화할 것

---

### [2026-06-02] 로그인 테스트 — localStorage 상태에 따른 UI 분기 미고려
**증상**: 회원가입 → 로그아웃 → 로그인 플로우에서 `getByLabel(/이메일/)` 타임아웃
**원인**: 회원가입 완료 시 계정 정보가 localStorage에 저장됨. 로그인 페이지는 저장된 계정이 있으면 account-list UI를 먼저 표시하고 이메일 입력 폼을 숨김. 테스트는 항상 이메일 입력 폼이 있다고 가정
**해결**: `.account-item` 클릭 → 비밀번호 입력 순서로 테스트 수정
**교훈**: E2E 테스트는 앱의 실제 상태(localStorage, 세션, 쿠키)를 고려해야 함. 이전 테스트 단계가 남긴 상태가 다음 단계 UI 분기에 영향을 줄 수 있음

---

## 인프라 / 배포

### [2026-05-17] Railway 배포 — Railpack 빌드 오류
**증상**: Railway가 `backend/` 폴더 안의 `railway.toml`을 못 찾고 Railpack으로 빌드 시도
**원인**: Railway는 레포 루트를 기준으로 `railway.toml`을 찾는다. `backend/railway.toml`은 인식 안 됨
**해결**:
- `railway.toml`을 레포 **루트**로 이동
- `dockerfilePath = "backend/Dockerfile"` 로 경로 지정
- `Dockerfile` COPY 경로도 `backend/` 접두사로 수정 (빌드 컨텍스트가 루트이므로)
```toml
[build]
builder = "dockerfile"
dockerfilePath = "backend/Dockerfile"
```
```dockerfile
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
COPY backend/src/ src/
```
**교훈**: Railway `railway.toml`은 반드시 레포 루트에 위치해야 함

---

### [2026-05-17] Railway 배포 — `server.port` 하드코딩으로 헬스체크 실패
**증상**: 빌드 성공 후 "Attempt #N failed with service unavailable" 반복
**원인**: Railway는 컨테이너에 `PORT` 환경변수를 주입하는데, `application.yml`에 `server.port: 8080` 고정값이면 Railway가 기대하는 포트와 불일치
**해결**: `application.yml`에서 `server.port: ${PORT:8080}` 으로 변경
**교훈**: Railway/Heroku 등 PaaS 배포 시 포트는 반드시 환경변수로 받아야 함

---

### [2026-05-17] Railway 배포 — JwtProvider 생성자 예외 (JWT_SECRET 미설정)
**증상**: `Error creating bean 'jwtProvider': Constructor threw exception`
**원인**: Railway Variables에 `JWT_SECRET`이 없어 `props.secret()`이 null → `Decoders.decode(null)` NPE
**해결**: Railway Variables 탭에 `JWT_SECRET` 추가
**교훈**: `application.yml`에 fallback 없는 `${VAR}` 항목은 배포 전 Variables 탭에서 전수 확인

---

### [2026-05-17] Railway 배포 — `Illegal base64 character: '_'`
**증상**: `io.jsonwebtoken.io.DecodingException: Illegal base64 character: '_'`
**원인**: Railway Variables에 입력한 JWT_SECRET이 URL-safe Base64(`_`, `-` 포함)인데 코드는 표준 `Decoders.BASE64` 사용. 표준 Base64는 `_`를 허용하지 않음
**해결**: `JwtProvider`에서 `Decoders.BASE64` → `Decoders.BASE64URL`로 변경
**교훈**: Railway UI에서 Base64 값을 입력할 때 `+`, `/`가 포함되면 복사 중 변형될 수 있음. URL-safe Base64 사용 권장

---

### [2026-05-17] Railway 배포 — `WeakKeyException: 56 bits`
**증상**: `io.jsonwebtoken.security.WeakKeyException: The specified key byte array is 56 bits`
**원인**: Railway Variables의 JWT_SECRET 값이 너무 짧음 (7 bytes). JJWT는 HMAC-SHA256에 최소 256 bits(32 bytes) 요구
**해결**: Base64 디코딩 대신 SHA-256 해시로 키를 파생하도록 변경 → 입력 길이/포맷 무관하게 항상 256-bit 키 생성
```java
private static byte[] sha256(String input) {
    return MessageDigest.getInstance("SHA-256")
        .digest(input.getBytes(StandardCharsets.UTF_8));
}
// 생성자에서:
this.secretKey = Keys.hmacShaKeyFor(sha256(props.secret().strip()));
```
**교훈**: JWT 시크릿은 Base64 디코딩에 의존하지 말고 SHA-256 파생으로 처리하면 환경변수 값 형식 문제에서 자유로워짐

---

### [2026-05-23] YAML 들여쓰기 오류로 `spring.mail` 설정 유실
**증상**: `JavaMailSender` 빈 생성 실패 — `No qualifying bean of type 'JavaMailSender' available`
**원인**: `mail:` 블록을 `spring:` 아래가 아닌 `jwt:` 아래에 들여쓰기해서 `jwt.mail.host`로 바인딩됨. `spring.mail.host`는 미설정 상태 → `MailSenderAutoConfiguration`이 실행되지 않아 `JavaMailSender` 빈 미생성
```yaml
# 잘못된 예 (jwt.mail.host로 바인딩)
jwt:
  secret: ${JWT_SECRET}
  mail:          # ← spring 아래가 아닌 jwt 아래
    host: localhost

# 올바른 예
spring:
  mail:
    host: localhost
jwt:
  secret: ${JWT_SECRET}
```
**교훈**: `spring.*` 하위 설정은 들여쓰기 한 단계만 틀려도 완전히 다른 키로 바인딩됨. 새 설정 블록 추가 시 루트 키(`spring:`, `server:` 등) 위치를 먼저 확인할 것

---

### [2026-05-23] `spring.mail` 활성화 시 `MailHealthIndicator`가 Railway Healthcheck 실패
**증상**: 앱 정상 기동 후에도 Railway Healthcheck가 주기적으로 실패. 메일 기능은 주석처리된 상태인데도 발생
**원인**: `spring.mail.host`가 설정되면 `JavaMailSender` 빈이 생성되고, Spring Boot Actuator가 `MailHealthIndicator`를 자동 등록. Healthcheck(`/actuator/health`) 호출마다 SMTP 서버에 연결을 시도하는데, Railway에는 Mailpit이 없으므로 `Connection refused` → 헬스 상태 `DOWN`
**해결**: 메일 헬스 인디케이터 비활성화
```yaml
management:
  health:
    mail:
      enabled: false
```
**교훈**: `spring.mail`을 설정하면 메일 기능을 쓰지 않아도 Actuator가 SMTP 핑을 보낸다. 메일 서버가 없는 환경에서는 `management.health.mail.enabled: false`를 함께 추가할 것

---

### [2026-05-17] Railway 배포 — `SPRING_DATASOURCE_URL` 형식 오류
**증상**: DB 연결 실패
**원인**: Railway Postgres의 `DATABASE_URL`은 `postgresql://...` 형식. Spring Boot JDBC는 `jdbc:postgresql://...` 형식 요구
**해결**: Railway Variables에서 URL을 직접 조합
```
SPRING_DATASOURCE_URL = jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
```
**교훈**: Railway `${{Postgres.DATABASE_URL}}`을 그대로 쓰면 안 됨. `jdbc:` 접두사를 붙여서 직접 조합할 것

---

### [2026-05-18] 크로스오리진 요청 401 — SameSite=Lax 쿠키 차단
**증상**: Vercel(프론트) → Railway(백엔드) POST 요청 시 401 Unauthorized. 로그인 후 일정 생성/조회 모두 실패
**원인**: 브라우저 SameSite=Lax 정책으로 크로스사이트 POST 요청에 httpOnly 쿠키가 전송되지 않음. 프론트·백엔드 도메인이 다르면(vercel.app ↔ railway.app) 쿠키 기반 인증은 근본적으로 불안정
**해결**: 쿠키 의존을 버리고 JWT를 `Authorization: Bearer` 헤더로 전달하도록 전환
```java
// JwtAuthenticationFilter.extractToken() — 헤더 우선, 쿠키 폴백
String authHeader = request.getHeader("Authorization");
if (authHeader != null && authHeader.startsWith("Bearer ")) {
    return authHeader.substring(7);
}
```
```javascript
// client.js — localStorage에서 토큰 읽어 헤더에 첨부
const token = localStorage.getItem('jwt')
headers: { ...(token && { 'Authorization': `Bearer ${token}` }) }
```
**교훈**: 프론트·백엔드 도메인이 다른 배포 환경에서는 처음부터 Bearer 헤더 방식으로 설계할 것. 쿠키 인증은 Same-Site 환경에서만 안정적

---

### [2026-05-18] 재배포 후 CORS OPTIONS 403
**증상**: 재배포 직후 모든 API 호출에 `OPTIONS ... 403` Preflight 실패
**원인 1**: `setAllowedOrigins()` 는 와일드카드(`*`) 패턴 지원 안 함 + `allowCredentials=true`와 함께 쓰면 Spring이 거부
**원인 2**: Spring Security `authorizeHttpRequests`에 `OPTIONS` 메서드 예외 규칙이 없어 preflight를 인증 필터가 차단
**원인 3**: `Authorization` 헤더가 `allowedHeaders` 목록에 없어 CORS가 차단
**해결**:
```java
// SecurityConfig — OPTIONS 전체 허용
.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

// CorsConfigurationSource — 와일드카드 지원 메서드 + Authorization 헤더 명시
config.setAllowedOriginPatterns(List.of(frontendUrl, "http://localhost:*"));
config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
config.setAllowCredentials(true);
```
**교훈**: CORS + JWT 헤더 조합 시 체크리스트 — ① `setAllowedOriginPatterns` 사용 ② `OPTIONS /**` permitAll ③ `Authorization` allowedHeaders 포함

---

### [2026-05-18] 배포 후 화면 공백 — localStorage 데이터 오염
**증상**: 재배포 후 초기 화면이 전혀 뜨지 않음. 콘솔에 `TypeError: Cannot read properties of undefined (reading '0')`
**원인**: 이전 배포에서 `briefy_accounts` localStorage 항목에 `{ email: undefined, name: undefined }` 형태의 잘못된 데이터가 저장됨. `account.name[0]` 접근 시 undefined 오류 → 화면 전체 크래시
**해결**: 브라우저 개발자도구에서 `localStorage.removeItem('briefy_accounts')` 실행
**교훈**: localStorage에 객체를 저장할 때 값이 유효한지 검증 후 저장할 것. `saveAccount` 함수에서 `email`이 빈 값이면 저장하지 않는 방어 코드 추가 권장

---

### [2026-05-17] Vercel 배포 — API URL이 Vercel 도메인에 붙어버리는 문제
**증상**: `https://briefy.vercel.app/briefy-production.up.railway.app/api/v1/...` 로 요청됨 → 405 오류
**원인**: `VITE_API_BASE_URL`에 `https://`를 빠뜨리고 도메인만 입력. 브라우저가 상대 경로로 해석해 Vercel 도메인 뒤에 붙임
**해결**: Vercel Environment Variables에서 `VITE_API_BASE_URL = https://briefy-production.up.railway.app` 으로 수정 후 재배포
**교훈**: `VITE_API_BASE_URL`은 반드시 `https://` 포함한 전체 URL

---

### [2026-06-02] Railway CLI `RAILWAY_TOKEN` 인증 지속 실패
**증상**: project deploy token, account token 모두 `Invalid RAILWAY_TOKEN` 오류. 토큰 재발급 후에도 동일
**원인**: Railway CLI 버전과 토큰 형식 간 호환성 문제로 추정. Railway GitHub 자동 배포 연동이 이미 설정된 경우 CI deploy job 자체가 불필요
**해결**: GitHub Actions deploy job 제거. Railway GitHub 연동이 push를 감지해 자동 배포
**교훈**: Railway GitHub 연동이 설정된 프로젝트에서 CI deploy job은 중복. 테스트는 CI에서, 배포는 Railway 연동으로 분리하면 관리 부담이 줄어듦

---

## 기타

### [2026-05-13] Claude Code에 공식 문서 연결하기 (Context7 MCP)
**증상**: Claude Code가 React 19, Spring Boot 4.0 등 최신 API를 옛날 패턴으로 생성함
**원인**: Claude의 훈련 데이터 컷오프 이후 변경된 API를 실시간으로 알 수 없음
**해결**:
1. Context7 MCP 설치
```bash
claude mcp add context7 -- npx -y @upstash/context7-mcp@latest
```
2. CLAUDE.md에 공식 문서 URL 명시
3. 프롬프트 끝에 `use context7` 추가하면 실시간 문서 기반으로 코드 생성

**교훈**: 최신 프레임워크 코드 생성 시 반드시 `use context7`를 붙일 것. CLAUDE.md에 버전과 문서 URL을 명시해두면 매 세션마다 컨텍스트 재설정 불필요
