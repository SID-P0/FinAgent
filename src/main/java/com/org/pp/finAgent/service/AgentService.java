package com.org.pp.finAgent.service;

import com.org.pp.finAgent.agent.Assistant;
import com.org.pp.finAgent.agent.tools.AgentTools;
import com.org.pp.finAgent.agent.tools.ChromeTools;
import com.org.pp.finAgent.automation.KeyboardMovement;
import com.org.pp.finAgent.configuration.GeminiConfig;
import com.org.pp.finAgent.controller.OCRController;

import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

@Service
public class AgentService {

    private static final String MODEL_NAME = "gemini-2.5-flash";

    private final GeminiConfig geminiConfig;
    private final OCRController ocrController;
    private final KeyboardMovement keyboardMovement;
    private final ChromeTools chromeTools;
    private Assistant assistant;

    public AgentService(GeminiConfig geminiConfig, OCRController ocrController,
            KeyboardMovement keyboardMovement, ChromeTools chromeTools) {
        this.geminiConfig = geminiConfig;
        this.ocrController = ocrController;
        this.keyboardMovement = keyboardMovement;
        this.chromeTools = chromeTools;
    }

    @PostConstruct
    private void initializeAgent() {
        // ChatModel model = GoogleAiGeminiChatModel.builder()
        // .apiKey(geminiConfig.getApiKey())
        // .modelName(MODEL_NAME)
        // .build();
        ChatModel model = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen2.5:7b-instruct")
                .temperature(0.4)
                .topP(0.9)
                .topK(40)
                .repeatPenalty(1.1)
                .numPredict(256)
                .build();

        AgentTools tools = new AgentTools(this.ocrController, this.keyboardMovement);

        assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(tools, chromeTools) // Register both AgentTools and ChromeTools
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
