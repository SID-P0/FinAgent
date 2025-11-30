package com.org.pp.finAgent.configuration;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GeminiConfig {
    @Value("${API_KEY}")
    private String API_KEY;

    @Bean
    public Client setupGeminiConfig(){
        return Client.builder().apiKey(API_KEY).build();
    }

    public String getApiKey() {
        return API_KEY;
    }
}
