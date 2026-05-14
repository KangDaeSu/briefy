package com.briefy.infra.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RagConfig {

    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.7;

    private static final String SYSTEM_PROMPT = """
            당신은 뉴스 브리핑 전문 AI 어시스턴트입니다.
            사용자의 관심사와 관련된 최신 뉴스를 바탕으로, 명확하고 간결한 브리핑을 제공합니다.

            규칙:
            - 반드시 제공된 컨텍스트(뉴스 기사)에 근거하여 답변하세요.
            - 컨텍스트에 없는 정보는 추측하지 마세요.
            - 한국어로 답변하되, 인명/기관명 등 고유명사는 원문 그대로 사용하세요.
            - 브리핑은 핵심 내용을 먼저 제시하고, 세부 내용은 항목별로 정리하세요.
            """;

    /**
     * RAG 파이프라인 ChatClient.
     * QuestionAnswerAdvisor가 사용자 쿼리를 벡터로 변환 → VectorStore에서 Top-K 검색
     * → 검색 결과를 시스템 컨텍스트에 주입 → ChatModel이 브리핑 생성.
     */
    @Bean
    public ChatClient ragChatClient(ChatModel chatModel, VectorStore vectorStore) {
        var qaAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build())
                .build();

        return ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(qaAdvisor)
                .build();
    }
}
