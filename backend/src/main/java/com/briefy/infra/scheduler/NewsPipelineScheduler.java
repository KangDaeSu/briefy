package com.briefy.infra.scheduler;

import com.briefy.domain.news.NewsCollectorService;
import com.briefy.domain.news.NewsEmbeddingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NewsPipelineScheduler {

    private static final Logger log = LoggerFactory.getLogger(NewsPipelineScheduler.class);

    private final NewsCollectorService collectorService;
    private final NewsEmbeddingService embeddingService;

    public NewsPipelineScheduler(NewsCollectorService collectorService,
                                 NewsEmbeddingService embeddingService) {
        this.collectorService = collectorService;
        this.embeddingService = embeddingService;
    }

    /** 매 시간 정각: NewsAPI에서 최신 헤드라인 수집 */
    @Scheduled(cron = "0 0 * * * *")
    public void collectNews() {
        log.info("[파이프라인] 뉴스 수집 시작");
        int saved = collectorService.collectTopHeadlines();
        log.info("[파이프라인] 뉴스 수집 완료: {}건 신규", saved);
    }

    /** 1분마다: embedded=false 기사 청킹 → 임베딩 처리 */
    @Scheduled(fixedDelay = 60_000)
    public void processEmbeddings() {
        embeddingService.processPendingEmbeddings();
    }
}
