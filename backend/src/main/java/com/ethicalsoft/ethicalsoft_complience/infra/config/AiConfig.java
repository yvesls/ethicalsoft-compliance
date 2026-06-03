package com.ethicalsoft.ethicalsoft_complience.infra.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
public class AiConfig {

    private boolean enabled = false;

    private int timeoutSeconds = 60;

    private int maxJustificationsPerRequest = 50;

    private String modelName = "llama-3.3-70b-versatile";

    private String baseUrl = "https://api.groq.com/openai";

    private double temperature = 0.3;

    private int maxTokens = 2048;

    private String tokenEncryptionKey;
}
