package com.briefy.domain.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 기사 한 건을 청킹 → 임베딩 → VectorStore 저장하는 트랜잭션 단위.
 * NewsEmbeddingService에서 기사별로 호출하여 실패 격리를 보장한다.
 */
@Component
public class NewsEmbeddingProcessor {

    private static final Logger log = LoggerFactory.getLogger(NewsEmbeddingProcessor.class);

    private final VectorStore vectorStore;
    private final NewsArticleRepository newsArticleRepository;
    private final TokenTextSplitter splitter;

    public NewsEmbeddingProcessor(VectorStore vectorStore, NewsArticleRepository newsArticleRepository) {
        this.vectorStore = vectorStore;
        this.newsArticleRepository = newsArticleRepository;
        this.splitter = TokenTextSplitter.builder()
            .withChunkSize(512)
            .withMinChunkSizeChars(50)
            .withMinChunkLengthToEmbed(5)
            .withMaxNumChunks(1000)
            .withKeepSeparator(true)
            .build();
    }

    @Transactional
    public void embed(NewsArticle article) {
        String text = buildText(article);
        if (text.isBlank()) {
            log.debug("임베딩할 텍스트 없음: articleId={}", article.getId());
            article.markEmbedded();
            newsArticleRepository.save(article);
            return;
        }

        Map<String, Object> metadata = Map.of(
            "articleId",   article.getId().toString(),
            "title",       article.getTitle(),
            "url",         article.getUrl(),
            "source",      article.getSource() != null ? article.getSource() : "",
            "publishedAt", article.getPublishedAt() != null ? article.getPublishedAt().toString() : ""
        );

        Document base = new Document(text, metadata);
        List<Document> chunks = splitter.apply(List.of(base));

        vectorStore.add(chunks);
        article.markEmbedded();
        newsArticleRepository.save(article);

        log.debug("임베딩 완료: articleId={}, chunks={}", article.getId(), chunks.size());
    }

    /** 임베딩 대상 텍스트 조합: title + description + content (문단 구분) */
    private String buildText(NewsArticle article) {
        StringBuilder sb = new StringBuilder(article.getTitle());
        if (article.getDescription() != null && !article.getDescription().isBlank()) {
            sb.append("\n\n").append(article.getDescription());
        }
        if (article.getContent() != null && !article.getContent().isBlank()) {
            sb.append("\n\n").append(article.getContent());
        }
        return sb.toString().strip();
    }
}
