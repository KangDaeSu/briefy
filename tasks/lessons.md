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

<!-- 예시 항목 — 실제 삽질 발생 시 추가 -->
<!--
### [2026-05-XX] PgVectorStore dimension mismatch
**증상**: 임베딩 저장 시 `ERROR: expected 1536 dimensions, not 3072`
**원인**: embedding 모델을 text-embedding-3-large(3072)로 바꿨는데 테이블 DDL은 1536 그대로
**해결**: `ALTER TABLE news_embeddings ALTER COLUMN embedding TYPE vector(3072)`
**교훈**: 모델 변경 시 반드시 Flyway 마이그레이션으로 컬럼 타입도 함께 변경
-->

---

## PostgreSQL / pgvector
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

---

## React 19

---

## 인프라 / 배포

---

## 기타

