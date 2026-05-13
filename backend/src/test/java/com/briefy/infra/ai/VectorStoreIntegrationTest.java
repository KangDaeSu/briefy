package com.briefy.infra.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

/**
 * PgVectorStore + Flyway 통합 테스트.
 * - pgvector/pgvector:pg18 컨테이너를 실제로 기동
 * - EmbeddingModel은 Mock (OpenAI API 호출 없음)
 * - 임베딩 저장 → 유사도 검색 전체 흐름 검증
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@ActiveProfiles("test")
class VectorStoreIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("pgvector/pgvector:pg18");

    // OpenAI 실제 호출 대신 고정 벡터 반환
    @MockBean
    EmbeddingModel embeddingModel;

    @Autowired
    VectorStore vectorStore;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void mockEmbedding() {
        float[] vec = new float[3072];
        Arrays.fill(vec, 0.1f);

        given(embeddingModel.call(any(EmbeddingRequest.class)))
                .willReturn(new EmbeddingResponse(List.of(new Embedding(vec, 0))));
        given(embeddingModel.dimensions()).willReturn(3072);
    }

    // ─────────────────────────────────────────
    // 스키마 검증
    // ─────────────────────────────────────────

    @Test
    @DisplayName("Flyway V1~V4 마이그레이션 후 모든 테이블이 존재해야 한다")
    void allTablesExistAfterMigration() {
        assertTableExists("users");
        assertTableExists("schedules");
        assertTableExists("news_articles");
        assertTableExists("news_embeddings");
        assertTableExists("briefs");
    }

    @Test
    @DisplayName("news_embeddings 테이블에 HNSW 인덱스가 생성되어 있어야 한다")
    void hnswIndexExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_indexes WHERE tablename = 'news_embeddings' AND indexname LIKE '%hnsw%'",
                Integer.class);
        assertThat(count).isGreaterThan(0);
    }

    @Test
    @DisplayName("schedules 테이블에 EXCLUDE 중복 방지 제약이 존재해야 한다")
    void scheduleOverlapConstraintExists() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_constraint WHERE conname = 'schedules_no_overlap'",
                Integer.class);
        assertThat(count).isEqualTo(1);
    }

    // ─────────────────────────────────────────
    // VectorStore CRUD
    // ─────────────────────────────────────────

    @Test
    @DisplayName("문서를 VectorStore에 저장하고 유사도 검색으로 조회할 수 있어야 한다")
    void addDocumentAndSearch() {
        var doc = new Document(
                "Spring AI는 Java 생태계에서 강력한 RAG 파이프라인을 제공합니다.",
                Map.of("articleId", "test-001", "source", "TechNews"));

        vectorStore.add(List.of(doc));

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("Java RAG 파이프라인")
                        .topK(3)
                        .build());

        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getText()).contains("Spring AI");
    }

    @Test
    @DisplayName("여러 문서를 배치로 저장하고 메타데이터로 필터링할 수 있어야 한다")
    void batchAddWithMetadataFilter() {
        var docs = List.of(
                new Document("AI가 의료 진단 정확도를 크게 향상시켰다.",
                        Map.of("category", "healthcare")),
                new Document("한국 주식 시장이 사상 최고치를 경신했다.",
                        Map.of("category", "finance")),
                new Document("새로운 언어 모델이 코딩 성능에서 인간을 넘어섰다.",
                        Map.of("category", "ai"))
        );

        vectorStore.add(docs);

        List<Document> results = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query("인공지능 기술 발전")
                        .topK(5)
                        .build());

        assertThat(results).isNotEmpty();
    }

    // ─────────────────────────────────────────
    // Helper
    // ─────────────────────────────────────────

    private void assertTableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = ?",
                Integer.class, tableName);
        assertThat(count).as("테이블 존재 여부: %s", tableName).isEqualTo(1);
    }
}
