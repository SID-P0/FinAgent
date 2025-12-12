package com.org.pp.finAgent.configuration;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for LM Studio local model.
 * Activates when model.provider=LOCAL in application.properties
 * 
 * LM Studio exposes an OpenAI-compatible API endpoint.
 * Configure your LM Studio to run on the specified base URL (default:
 * http://127.0.0.1:9898)
 */
@Configuration
@ConditionalOnProperty(name = "model.provider", havingValue = "LOCAL")
public class LMStudioConfig {

    @Value("${lmstudio.base-url:http://127.0.0.1:9898}")
    private String baseUrl;

    @Value("${lmstudio.api-key:not-needed}")
    private String apiKey;

    @Bean
    @Primary
    public Client setupLMStudioClient() {
        // LM Studio provides an OpenAI-compatible endpoint
        // Configure the client to point to your local LM Studio instance
        return Client.builder()
                .apiKey(apiKey) // LM Studio doesn't need a real API key
                .build();
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }
}
