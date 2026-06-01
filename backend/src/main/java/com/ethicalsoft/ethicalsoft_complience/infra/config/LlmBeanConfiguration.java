package com.ethicalsoft.ethicalsoft_complience.infra.config;

import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.AiDataSanitizer;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.AiPromptBuilder;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.GroqLlmAdapter;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.llm.NoOpLlmAdapter;
import com.ethicalsoft.ethicalsoft_complience.adapters.out.mongo.repository.AiGenerationCacheRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import com.ethicalsoft.ethicalsoft_complience.application.port.ai.LlmAnalysisPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Slf4j
public class LlmBeanConfiguration {

    @Bean
    @ConditionalOnMissingBean(LlmAnalysisPort.class)
    public LlmAnalysisPort noOpLlmAdapter() {
        log.info("[llm-config] IA desabilitada — usando NoOpLlmAdapter");
        return new NoOpLlmAdapter();
    }

    @Configuration
    @ConditionalOnProperty(name = "app.ai.enabled", havingValue = "true")
    @Import(OpenAiChatAutoConfiguration.class)
    static class GroqLlmEnabledConfiguration {

        @Bean
        public LlmAnalysisPort groqLlmAdapter(ChatModel chatModel,
                                               AiDataSanitizer sanitizer,
                                               AiPromptBuilder promptBuilder,
                                               AiConfig aiConfig,
                                               CircuitBreakerRegistry circuitBreakerRegistry,
                                               AiGenerationCacheRepository generationCacheRepository) {
            log.info("[llm-config] IA habilitada — Groq Cloud (modelo: {})", aiConfig.getModelName());
            return new GroqLlmAdapter(chatModel, sanitizer, promptBuilder, aiConfig,
                    circuitBreakerRegistry, generationCacheRepository);
        }
    }
}

