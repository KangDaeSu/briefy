package com.briefy.domain.news;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, UUID> {

    boolean existsByUrlHash(String urlHash);

    // 임베딩 배치 처리용: 미처리 기사 최대 20개
    List<NewsArticle> findTop20ByEmbeddedFalseOrderByCreatedAtAsc();
}
