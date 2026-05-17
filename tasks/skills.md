# briefy — 작업 스킬 가이드

이 코드베이스에서 자주 쓰는 패턴의 실전 레시피.

---

## 백엔드

### 새 도메인 엔티티 추가

```java
// 엔티티: protected 기본 생성자, 공개 생성자로 필수 필드 강제
@Entity
@Table(name = "some_things")
public class SomeThing {

    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME) // UUIDv7
    private UUID id;

    @NonNull
    @Column(nullable = false)
    private String name;

    @Nullable
    private String description;

    @NonNull
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected SomeThing() {}

    public SomeThing(@NonNull String name) {
        this.name = name;
    }

    @PrePersist
    private void prePersist() { createdAt = OffsetDateTime.now(); }

    // getter만, setter 없음
}

// Repository
public interface SomeThingRepository extends JpaRepository<SomeThing, UUID> { }

// Service: 클래스에 readOnly, 쓰기 메서드에만 @Transactional
@Service
@Transactional(readOnly = true)
public class SomeThingService {
    private final SomeThingRepository repo;
    public SomeThingService(SomeThingRepository repo) { this.repo = repo; }

    @Transactional
    public SomeThingResponse create(SomeThingRequest req) {
        return SomeThingResponse.from(repo.save(new SomeThing(req.name())));
    }
}
```

---

### 새 API 엔드포인트 추가

```java
// @RequestMapping("/api/v1/...") 경로 접두사로 버전 관리
@RestController
@RequestMapping("/api/v1/some-things")
public class SomeThingController {

    private final SomeThingService service;

    public SomeThingController(SomeThingService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SomeThingResponse> create(
        @RequestHeader("X-User-Id") UUID userId,  // Phase 3 이전 임시 인증
        @Valid @RequestBody SomeThingRequest request
    ) {
        return ApiResponse.ok(service.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<SomeThingResponse> getOne(@PathVariable UUID id) {
        return ApiResponse.ok(service.getOne(id));
    }
}

// DTO: Java record
public record SomeThingRequest(@NotBlank String name, @Nullable String description) {}

public record SomeThingResponse(UUID id, String name) {
    public static SomeThingResponse from(SomeThing s) {
        return new SomeThingResponse(s.getId(), s.getName());
    }
}
```

---

### 에러 처리

```java
// BriefyErrorCode에 새 에러 추가 후
var thing = repo.findById(id)
    .orElseThrow(() -> new BriefyException(BriefyErrorCode.SOME_THING_NOT_FOUND));

// GlobalExceptionHandler가 자동으로 ApiResponse.error(errorCode) 반환
```

---

### Flyway 마이그레이션 추가

파일명: `backend/src/main/resources/db/migration/V{N}__설명.sql`

```sql
-- UUIDv7: PG18 네이티브 함수는 uuidv7() (uuid_generate_v7() 아님!)
CREATE TABLE some_things (
    id          UUID         NOT NULL DEFAULT uuidv7(),
    name        VARCHAR(255) NOT NULL,     -- CHAR(n) 금지: Hibernate validate bpchar 불일치
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),

    CONSTRAINT some_things_pk PRIMARY KEY (id)
);
```

---

## 테스트

### Testcontainers 통합 테스트 (2.x)

```java
@SpringBootTest
@Testcontainers                         // testcontainers-junit-jupiter 아티팩트 필요
@ActiveProfiles("test")
class SomeIntegrationTest {

    @Container
    @ServiceConnection                  // spring-boot-testcontainers가 DataSource 자동 설정
    static PostgreSQLContainer<?> postgres =
        new PostgreSQLContainer<>("postgres:18");

    @Test
    void something() { ... }
}
```

---

### @MockitoBean (Spring Boot 4.0)

```java
// @MockBean 제거됨 — 반드시 @MockitoBean 사용
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class SomeServiceTest {

    @MockitoBean
    SomeDependency dependency;

    @Autowired
    SomeService service;

    @Test
    void test() {
        given(dependency.getData()).willReturn("mock");
        assertThat(service.process()).isEqualTo("mock");
    }
}
```

---

## 프론트엔드

### API 호출 패턴

```js
// src/api/client.js의 api 래퍼 사용
import { api } from '../api/client'

const TEST_USER_ID = '11111111-1111-1111-1111-111111111111' // Phase 3 이전 임시
const headers = { 'X-User-Id': TEST_USER_ID }

export const someThingsApi = {
  list: () => api.get('/api/v1/some-things', { headers }),
  getOne: (id) => api.get(`/api/v1/some-things/${id}`, { headers }),
  create: (data) => api.post('/api/v1/some-things', data, { headers }),
  update: (id, data) => api.patch(`/api/v1/some-things/${id}`, data, { headers }),
  delete: (id) => api.delete(`/api/v1/some-things/${id}`, { headers }),
}
```

---

### 컴포넌트 데이터 로딩 패턴

```jsx
// Vite SPA — useState + useEffect + fetch 래퍼
import { useState, useEffect, useCallback } from 'react'
import { someThingsApi } from '../api/someThings'

export default function SomeThingPage() {
  const [items, setItems] = useState([])
  const [loading, setLoading] = useState(false)

  const fetchItems = useCallback(async () => {
    setLoading(true)
    try {
      const res = await someThingsApi.list()
      setItems(res.data ?? [])
    } catch (e) {
      console.error('조회 실패', e)
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => { fetchItems() }, [fetchItems])

  if (loading) return <div>로딩 중…</div>
  return <ul>{items.map(item => <li key={item.id}>{item.name}</li>)}</ul>
}
```

---

## 자주 하는 실수

| 실수 | 올바른 방법 |
|------|-----------|
| `uuid_generate_v7()` | `uuidv7()` (PG18 네이티브) |
| `CHAR(n)` DDL | `VARCHAR(n)` (Hibernate validate 호환) |
| `@MockBean` | `@MockitoBean` (Spring Boot 4.0) |
| `spring-boot-starter-web` | `spring-boot-starter-webmvc` (Boot 4.0) |
| `artifactId: postgresql` (Testcontainers 1.x) | `testcontainers-postgresql` + `testcontainers-junit-jupiter` |
