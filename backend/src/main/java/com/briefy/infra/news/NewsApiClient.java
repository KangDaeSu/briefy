package com.briefy.infra.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class NewsApiClient {

    private static final Logger log = LoggerFactory.getLogger(NewsApiClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public NewsApiClient(
        RestClient.Builder builder,
        @Value("${news.api.key:}") String apiKey
    ) {
        this.restClient = builder
            .baseUrl("https://newsapi.org/v2")
            .build();
        this.apiKey = apiKey;
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** 상위 뉴스 헤드라인 수집 */
    public NewsApiResponse fetchTopHeadlines(String language, int pageSize) {
        log.debug("NewsAPI top-headlines 호출: language={}, pageSize={}", language, pageSize);
        return restClient.get()
            .uri("/top-headlines?language={lang}&pageSize={size}&apiKey={key}",
                language, pageSize, apiKey)
            .retrieve()
            .body(NewsApiResponse.class);
    }

    /** 키워드 기반 뉴스 검색 (Phase 2-3 RAG에서 관심사 매핑용) */
    public NewsApiResponse fetchEverything(String query, String language, int pageSize) {
        log.debug("NewsAPI everything 호출: q={}, language={}", query, language);
        return restClient.get()
            .uri("/everything?q={q}&language={lang}&sortBy=publishedAt&pageSize={size}&apiKey={key}",
                query, language, pageSize, apiKey)
            .retrieve()
            .body(NewsApiResponse.class);
    }
}
