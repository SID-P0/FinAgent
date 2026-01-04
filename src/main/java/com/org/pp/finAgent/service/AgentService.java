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
    private final ChromeTools chromeTools;
    private final AgentTools agentTools;
    private Assistant assistant;
    private MessageWindowChatMemory chatMemory;

    public AgentService(GeminiConfig geminiConfig, OCRController ocrController,
            KeyboardMovement keyboardMovement, ChromeTools chromeTools, AgentTools agentTools) {
        this.geminiConfig = geminiConfig;
        this.agentTools = agentTools;
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
                .modelName("qwen2.5:7b")
                .build();

        // AgentTools tools = new AgentTools(this.keyboardMovement);

        // Store reference to memory so we can clear it later
        chatMemory = MessageWindowChatMemory.withMaxMessages(10);

        assistant = AiServices.builder(Assistant.class)
                .chatModel(model)
                .tools(agentTools, chromeTools) // Register both AgentTools and ChromeTools
                .chatMemory(chatMemory)
                .build();
    }

    /**
     * Clears the chat memory/context window.
     * Call this before generating a new plan to start fresh.
     */
    public void clearMemory() {
        if (chatMemory != null) {
            chatMemory.clear();
        }
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
