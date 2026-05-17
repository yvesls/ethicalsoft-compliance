package com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.model;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AiInsightResult {

    private final boolean available;
    private final String content;
    private final String model;
    private final LocalDateTime generatedAt;
    private final String fallbackMessage;

    public static AiInsightResult success(String content, String model) {
        return AiInsightResult.builder()
                .available(true)
                .content(content)
                .model(model)
                .generatedAt(LocalDateTime.now())
                .build();
    }

    public static AiInsightResult unavailable(String fallbackMessage) {
        return AiInsightResult.builder()
                .available(false)
                .fallbackMessage(fallbackMessage)
                .generatedAt(LocalDateTime.now())
                .build();
    }
}

