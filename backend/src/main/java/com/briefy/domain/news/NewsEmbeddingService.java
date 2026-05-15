package com.briefy.domain.news;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NewsEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(NewsEmbeddingService.class);

    private final NewsArticleRepository newsArticleRepository;
    private final NewsEmbeddingProcessor processor;

    public NewsEmbeddingService(NewsArticleRepository newsArticleRepository,
                                NewsEmbeddingProcessor processor) {
        this.newsArticleRepository = newsArticleRepository;
        this.processor = processor;
    }

    /**
     * embedded=false 기사를 배치(최대 20건)로 가져와 청킹 → 임베딩 처리한다.
     * 기사별로 독립 트랜잭션(NewsEmbeddingProcessor)이므로 개별 실패가 전체에 영향을 주지 않는다.
     */
    public int processPendingEmbeddings() {
        List<NewsArticle> pending = newsArticleRepository.findTop20ByEmbeddedFalseOrderByCreatedAtAsc();
        if (pending.isEmpty()) return 0;

        log.info("임베딩 배치 시작: {}건", pending.size());
        int success = 0;

        for (NewsArticle article : pending) {
            try {
                processor.embed(article);
                success++;
            } catch (Exception e) {
                log.error("임베딩 실패 (articleId={}): {}", article.getId(), e.getMessage());
            }
        }

        log.info("임베딩 배치 완료: {}/{}건 성공", success, pending.size());
        return success;
    }
}
