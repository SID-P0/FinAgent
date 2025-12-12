package com.org.pp.finAgent.configuration;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for Gemini API.
 * This is the default configuration when model.provider is not specified.
 * To use LM Studio instead, set model.provider=LOCAL in application.properties
 */
@Configuration
@ConditionalOnProperty(name = "model.provider", havingValue = "GEMINI", matchIfMissing = true)
public class GeminiConfig {
    @Value("${API_KEY}")
    private String API_KEY;

    @Bean
    public Client setupGeminiConfig() {
        return Client.builder().apiKey(API_KEY).build();
    }

    public String getApiKey() {
        return API_KEY;
    }
}
