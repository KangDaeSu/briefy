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

### [2026-05-15] QuestionAnswerAdvisor 패키지 이동 및 Builder 전환 (Spring AI 2.0.0-M6)
**증상**: `import org.springframework.ai.chat.client.advisor.QuestionAnswerAdvisor` 컴파일 오류
**원인**: 2.0.0-M6에서 패키지가 `advisor.vectorstore`로 이동, 생성자도 제거되고 Builder로 교체
**해결**:
```java
// Before (1.x)
new QuestionAnswerAdvisor(vectorStore, searchRequest)

// After (2.0.0-M6)
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
QuestionAnswerAdvisor.builder(vectorStore).searchRequest(searchRequest).build()
```
**교훈**: Spring AI 마이그레이션 시 advisor 패키지 경로 반드시 확인

---

## PostgreSQL / pgvector

### [2026-05-15] PG18 UUIDv7 함수명: uuid_generate_v7() 아님
**증상**: Flyway 마이그레이션 실패 — `ERROR: function uuid_generate_v7() does not exist`
**원인**: `uuid_generate_v7()`은 `pg_uuidv7` 확장의 함수명. PG18 네이티브 빌트인은 `uuidv7()`
**해결**: 모든 DDL에서 `DEFAULT uuid_generate_v7()` → `DEFAULT uuidv7()`로 교체
**교훈**: PG18 네이티브 UUIDv7은 `uuidv7()`. 확장 없이 쓸 수 있지만 이름이 다름. 문서 확인 필수

---

### [2026-05-15] pgvector HNSW 인덱스 2000차원 한계 (vector 타입)
**증상**: `ERROR: column cannot have more than 2000 dimensions for hnsw index`
**원인**: pgvector의 `vector` 타입은 HNSW 인덱스 최대 2000차원. text-embedding-3-large는 3072차원
**해결**: `halfvec` 캐스팅 함수 인덱스 사용 (pgvector 0.7.0+, halfvec 최대 4000차원)
```sql
-- Before (실패)
CREATE INDEX ... USING hnsw (embedding vector_cosine_ops);

-- After (성공)
CREATE INDEX ... USING hnsw ((embedding::halfvec(3072)) halfvec_cosine_ops);
```
**교훈**: 2000차원 초과 임베딩은 HNSW에 halfvec 캐스팅 함수 인덱스 필수. 컬럼은 `vector` 유지 가능

---

### [2026-05-15] PostgreSQL CHAR(n) vs VARCHAR(n) Hibernate 스키마 검증 실패
**증상**: `SchemaManagementException: wrong column type; found [bpchar], but expecting [varchar(64)]`
**원인**: SQL `CHAR(64)`는 PostgreSQL 내부적으로 `bpchar` 타입. Hibernate Java `String` 필드는 `varchar` 기대
**해결**: DDL을 `CHAR(64)` → `VARCHAR(64)`로 변경
**교훈**: Hibernate `ddl-auto: validate` 사용 시 `CHAR` 컬럼은 반드시 `VARCHAR`로 선언

---

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

### [2026-05-15] @MockitoBean으로 인터페이스 default 메서드 모킹 시 위임 안 됨
**증상**: `vectorStore.similaritySearch()` 결과가 항상 빈 리스트 반환
**원인**: `@MockitoBean`으로 `EmbeddingModel` 인터페이스를 모킹하면 default 메서드가 실제 구현을 호출하지 않고 null 반환. 배치 add 시 `call()`이 embeddings를 1개만 반환해 `IndexOutOfBoundsException` 발생
**해결**: `@MockitoBean` 대신 `@TestConfiguration`에 실제 구현체 제공
```java
@TestConfiguration
static class TestConfig {
    @Bean @Primary
    EmbeddingModel fixedVectorEmbeddingModel() {
        return new EmbeddingModel() {
            @Override
            public EmbeddingResponse call(EmbeddingRequest req) {
                var embeddings = IntStream.range(0, req.getInstructions().size())
                    .mapToObj(i -> new Embedding(fixedVec(), i)).toList();
                return new EmbeddingResponse(embeddings);
            }
            @Override public float[] embed(Document doc) { return fixedVec(); }
            @Override public int dimensions() { return 3072; }
        };
    }
}
```
**교훈**: 인터페이스 default 메서드가 많고 상호 의존하는 경우 Mock보다 실구현체 테스트 더블이 안전

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
