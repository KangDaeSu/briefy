package com.briefy.domain.news;

import com.briefy.infra.news.NewsApiClient;
import com.briefy.infra.news.NewsApiResponse;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class NewsCollectorService {

    private static final Logger log = LoggerFactory.getLogger(NewsCollectorService.class);

    private final NewsApiClient newsApiClient;
    private final NewsArticleRepository newsArticleRepository;
    private final String language;
    private final int pageSize;

    public NewsCollectorService(
        NewsApiClient newsApiClient,
        NewsArticleRepository newsArticleRepository,
        @Value("${news.api.language:ko}") String language,
        @Value("${news.api.page-size:20}") int pageSize
    ) {
        this.newsApiClient = newsApiClient;
        this.newsArticleRepository = newsArticleRepository;
        this.language = language;
        this.pageSize = pageSize;
    }

    @Transactional
    public int collectTopHeadlines() {
        if (!newsApiClient.isConfigured()) {
            log.warn("NEWS_API_KEY 미설정 — 뉴스 수집 생략");
            return 0;
        }

        NewsApiResponse response;
        try {
            response = newsApiClient.fetchTopHeadlines(language, pageSize);
        } catch (Exception e) {
            log.error("뉴스 수집 실패: {}", e.getMessage());
            return 0;
        }

        if (!"ok".equals(response.status()) || response.articles() == null) {
            log.warn("NewsAPI 비정상 응답: status={}", response.status());
            return 0;
        }

        int saved = 0;
        for (NewsApiResponse.Article article : response.articles()) {
            if (isInvalid(article)) continue;
            try {
                saved += saveIfNew(article);
            } catch (Exception e) {
                log.warn("기사 저장 실패 (url={}): {}", article.url(), e.getMessage());
            }
        }

        log.info("뉴스 수집 완료: {}건 신규 저장 (총 {}건 처리)", saved, response.articles().size());
        return saved;
    }

    private int saveIfNew(NewsApiResponse.Article article) {
        String urlHash = sha256(article.url());
        if (newsArticleRepository.existsByUrlHash(urlHash)) {
            return 0; // 중복 기사
        }

        NewsArticle entity = new NewsArticle(article.url(), urlHash, sanitize(article.title()));
        entity.setSource(sourceName(article.source()));
        entity.setAuthor(article.author());
        entity.setDescription(article.description());
        entity.setContent(stripTruncationMarker(article.content()));
        entity.setPublishedAt(parsePublishedAt(article.publishedAt()));

        newsArticleRepository.save(entity);
        return 1;
    }

    private boolean isInvalid(NewsApiResponse.Article article) {
        return article.url() == null || article.url().isBlank()
            || article.title() == null || article.title().isBlank()
            || "[Removed]".equalsIgnoreCase(article.title());
    }

    /** NewsAPI free tier: content 끝에 "[+NNNN chars]" 마커 제거 */
    @Nullable
    private String stripTruncationMarker(@Nullable String content) {
        if (content == null) return null;
        return content.replaceAll("\\[\\+\\d+ chars\\]$", "").strip();
    }

    private @Nullable String sourceName(NewsApiResponse.@Nullable Source source) {
        return source != null ? source.name() : null;
    }

    private String sanitize(String text) {
        return text.strip();
    }

    @Nullable
    private OffsetDateTime parsePublishedAt(@Nullable String publishedAt) {
        if (publishedAt == null || publishedAt.isBlank()) return null;
        try {
            return OffsetDateTime.parse(publishedAt);
        } catch (Exception e) {
            return null;
        }
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(64);
            for (byte b : hash) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
