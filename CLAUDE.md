# briefy

지능형 일정 관리 + RAG 뉴스 요약 서비스.
사용자의 관심사 기반으로 뉴스를 벡터 검색하여 요약하고, 일정과 연동해 브리핑을 제공한다.

## Tech Stack

| Layer     | Technology                          |
|-----------|-------------------------------------|
| Frontend  | React 19, TypeScript, Vite          |
| Backend   | Spring Boot 4.0, Spring AI          |
| Database  | PostgreSQL 18 + pgvector 0.8.2      |
| AI        | Spring AI (RAG pipeline, embedding) |

## Project Structure

```
briefy/
├── frontend/          # React 19 앱 (Vite)
│   ├── src/
│   │   ├── components/
│   │   ├── pages/
│   │   ├── hooks/
│   │   └── api/
│   └── package.json
├── backend/           # Spring Boot 4.0 서버
│   ├── src/main/java/com/briefy/
│   │   ├── domain/
│   │   │   ├── schedule/   # 일정 도메인
│   │   │   ├── news/       # 뉴스 수집/임베딩 도메인
│   │   │   └── brief/      # RAG 브리핑 도메인
│   │   ├── infra/
│   │   │   ├── ai/         # Spring AI 설정 및 RAG 파이프라인
│   │   │   └── vector/     # pgvector 연동
│   │   └── api/            # REST 컨트롤러
│   └── pom.xml
├── tasks/
│   ├── plan.md        # 구현 계획
│   └── lessons.md     # 삽질 기록
└── CLAUDE.md
```

## Commands

### Frontend
```bash
cd frontend
npm install
npm run dev          # 개발 서버 (localhost:5173)
npm run build
npm run typecheck    # tsc --noEmit
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
# pgvector 확장 활성화 (최초 1회)
psql -U postgres -d briefy -c "CREATE EXTENSION IF NOT EXISTS vector;"
```

## Key Conventions

### API Design
- REST 엔드포인트: Spring Boot 4.0의 `@ApiVersion` 어노테이션으로 버전 관리 (예: `/v1/schedules`)
- 모든 응답은 `ApiResponse<T>` wrapper 사용
- 에러 코드는 `BriefyErrorCode` enum으로 관리

### RAG Pipeline
1. 뉴스 수집 → 청크 분할 → 임베딩 생성 (Spring AI EmbeddingModel)
2. pgvector에 저장 (`VectorStore` 구현체)
3. 쿼리 시 유사도 검색 → ChatModel에 컨텍스트 주입 → 브리핑 생성

### Vector Schema
```sql
CREATE TABLE news_embeddings (
    -- UUIDv7: 시간 순 정렬 가능 → B-tree 인덱스 단편화 최소화, 삽입 성능 개선
    id          UUID PRIMARY KEY DEFAULT uuid_generate_v7(),
    content     TEXT NOT NULL,
    embedding   vector(3072),  -- OpenAI text-embedding-3-large 기준
    metadata    JSONB,
    created_at  TIMESTAMPTZ DEFAULT now()
);
CREATE INDEX ON news_embeddings USING hnsw (embedding vector_cosine_ops);
```

### Database Conventions
- PK: 모든 테이블의 Primary Key는 UUIDv7 사용 (`uuid_generate_v7()`)
- Schedule: 일정 중복 방지를 위해 DB 엔진 레벨에서 `WITHOUT OVERLAPS` 제약 조건 필수 적용

### Backend Conventions
- 런타임 NullPointerException 방지를 위해 `org.jspecify.annotations` 기반의 엄격한 Null 체크 수행

### Frontend State
- 데이터 페칭: React 19 Server Components (RSC) 및 `use()` Hook 최우선 사용
- 폼 및 상태 변이: `useActionState`와 `useFormStatus`를 활용한 Server Actions 및 낙관적 업데이트(Optimistic UI) 구현
- 클라이언트 상태: 상호작용이 필수적인 컴포넌트(`'use client'`)에 한해 최소한의 `useState` 사용

## Environment Variables

### Backend (`backend/.env` or `application-local.properties`)
```
OPENAI_API_KEY=...
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/briefy
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=...
NEWS_API_KEY=...
```

### Frontend (`frontend/.env.local`)
```
VITE_API_BASE_URL=http://localhost:8080
```

## Development Notes

- Spring Boot 4.0은 Java 21+ 필수, virtual threads (Loom) 기본 활성화
- pgvector 0.8.2는 HNSW 인덱스를 기본 권장, IVFFlat보다 검색 품질 우수
- Spring AI의 `VectorStore`는 `PgVectorStore` 구현체 사용
- React 19의 `use()` hook과 Suspense를 데이터 페칭에 적극 활용
- 뉴스 임베딩 배치 처리는 Spring Batch 또는 `@Scheduled` + 비동기 처리
