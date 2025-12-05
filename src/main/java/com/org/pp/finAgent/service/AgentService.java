package com.org.pp.finAgent.service;

import com.org.pp.finAgent.agent.Assistant;
import com.org.pp.finAgent.agent.tools.AgentTools;
import com.org.pp.finAgent.configuration.GeminiConfig;
import com.org.pp.finAgent.controller.OCRController;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private static final String MODEL_NAME = "gemini-2.5-flash";

    private final GeminiConfig geminiConfig;
    private final OCRController ocrController;
    private Assistant assistant;

    public AgentService(GeminiConfig geminiConfig, OCRController ocrController) {
        this.geminiConfig = geminiConfig;
        this.ocrController = ocrController;
    }

    @PostConstruct
    private void initializeAgent() {
        ChatModel model = dev.langchain4j.model.googleai.GoogleAiGeminiChatModel.builder()
                .apiKey(geminiConfig.getApiKey())
                .modelName(MODEL_NAME)
                .build();

        AgentTools tools = new AgentTools(this.ocrController);

        assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(tools)
                .chatMemory(MessageWindowChatMemory.withMaxMessages(10))
                .build();
    }

    /**
     * Interacts with the pre-configured LangChain agent.
     * 
     * @param prompt The user's message to the agent.
     * @return The agent's response.
     */
    public String chatWithAgent(String prompt) {
        if (assistant == null) {
            throw new IllegalStateException("Agent has not been initialized. Check the configuration.");
        }
        return assistant.chat(prompt);
    }
}
