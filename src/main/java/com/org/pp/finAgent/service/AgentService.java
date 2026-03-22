package com.org.pp.finAgent.service;

import com.org.pp.finAgent.agent.tools.AgentTools;
import com.org.pp.finAgent.agent.tools.ChromeTools;
import com.org.pp.finAgent.automation.KeyboardMovement;
import com.org.pp.finAgent.configuration.GeminiConfig;
import com.org.pp.finAgent.controller.OCRController;

import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.tool.ToolExecutor;
import dev.langchain4j.agent.tool.ToolSpecifications;
import dev.langchain4j.service.tool.DefaultToolExecutor;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentService {

    private static final String MODEL_NAME = "gemini-2.5-flash";

    private final GeminiConfig geminiConfig;
    private final ChromeTools chromeTools;
    private final AgentTools agentTools;
    
    private ChatModel chatModel;
    private List<ToolSpecification> toolSpecifications;
    private Map<String, ToolExecutor> toolExecutors;
    
    // We will manage our own message lists per execution
    private List<ChatMessage> currentExecutionMessages;

    public AgentService(GeminiConfig geminiConfig, OCRController ocrController,
            KeyboardMovement keyboardMovement, ChromeTools chromeTools, AgentTools agentTools) {
        this.geminiConfig = geminiConfig;
        this.agentTools = agentTools;
        this.chromeTools = chromeTools;
        this.toolSpecifications = new ArrayList<>();
        this.toolExecutors = new HashMap<>();
        this.currentExecutionMessages = new ArrayList<>();
    }

    @PostConstruct
    private void initializeAgent() {
        this.chatModel = OllamaChatModel.builder()
                .baseUrl("http://localhost:11434")
                .modelName("qwen2.5:7b")
                .numCtx(16096)
                .build();

        // 1. Build ToolSpecifications from our classes
        toolSpecifications.addAll(ToolSpecifications.toolSpecificationsFrom(agentTools));
        toolSpecifications.addAll(ToolSpecifications.toolSpecificationsFrom(chromeTools));

        // 2. Build ToolExecutors (we use DefaultToolExecutor which finds the method by @Tool annotation)
        toolSpecifications.forEach(spec -> {
            // Check which object has the method for this tool specification
            try {
                // Try agentTools first
                boolean foundInAgentTools = false;
                for (java.lang.reflect.Method m : agentTools.getClass().getMethods()) {
                    if (m.getName().equals(spec.name())) {
                        toolExecutors.put(spec.name(), new DefaultToolExecutor(agentTools, m));
                        foundInAgentTools = true;
                        break;
                    }
                }
                
                // If not in agentTools, try chromeTools
                if (!foundInAgentTools) {
                    for (java.lang.reflect.Method m : chromeTools.getClass().getMethods()) {
                        if (m.getName().equals(spec.name())) {
                            toolExecutors.put(spec.name(), new DefaultToolExecutor(chromeTools, m));
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Failed to bind tool executor for: " + spec.name());
            }
        });
    }

    /**
     * Clears the current execution message list.
     */
    public void clearMemory() {
        if (currentExecutionMessages != null) {
            currentExecutionMessages.clear();
        }
        if (agentTools != null) {
            agentTools.resetScrollState();
        }
    }

    /**
     * Helper method to convert tool specifications into a readable string
     * for the planner, so it knows what tools exist without registering them as JSON functions.
     */
    private String getToolsDescription() {
        StringBuilder sb = new StringBuilder("Available Tools:\n");
        for (ToolSpecification spec : toolSpecifications) {
            sb.append("- ").append(spec.name());
            
            // Append parameters
            if (spec.parameters() != null && spec.parameters().properties() != null) {
                sb.append("(");
                List<String> paramNames = new ArrayList<>(spec.parameters().properties().keySet());
                sb.append(String.join(", ", paramNames));
                sb.append(")");
            } else {
                sb.append("()");
            }
            
            sb.append(": ").append(spec.description()).append("\n");
        }
        return sb.toString();
    }

    /**
     * Phase 1: Planning
     * Generate a plan WITHOUT executing tools, but provide the tool specifications
     * as plain text so the LLM knows what tools are available without triggering
     * function-calling behavior.
     * 
     * @param prompt The user's task description
     * @return The generated text plan
     */
    public String generatePlan(String prompt) {
        clearMemory();
        
        String toolDocs = getToolsDescription();
        
        SystemMessage systemMessage = SystemMessage.from(
                "You are a highly structured planning assistant. Your job is to create step-by-step execution plans.\n" +
                "You have access to the following tools:\n\n" + toolDocs + "\n" +
                "CRITICAL INSTRUCTIONS:\n" +
                "1. First, think step-by-step about how to solve the task. Wrap your thinking in <thought>...</thought> tags.\n" +
                "2. Second, output a numbered list of the exact tool calls to execute.\n" +
                "3. Each line MUST strictly follow this exact format: Number. ToolName: argument_value\n" +
                "4. If a tool takes no arguments, omit the colon.\n" +
                "5. SCROLLING RULE: If you need to scroll down multiple times, YOU MUST increment the scrollPercentage by 100 each time (e.g., 100, then 200, then 300) because it is an absolute coordinate.\n" +
                "6. DO NOT output any text after the numbered list.\n\n" +
                "EXAMPLE OUTPUT:\n" +
                "<thought>\n" +
                "The user wants news about bajaj stock. I need to open a new tab, search, click links, and scroll through the page.\n" +
                "</thought>\n" +
                "1. openNewTab\n" +
                "2. navigateToUrl: https://www.google.com/\n" +
                "3. searchInChrome: bajaj stock\n" +
                "4. clickAllBlueLinks\n" +
                "5. scrollPercentage: 100\n" +
                "6. clickAllBlueLinks\n" +
                "7. scrollPercentage: 200\n" +
                "8. clickAllBlueLinks"
        );
                
        UserMessage userMessage = UserMessage.from("Task: " + prompt);
        
        currentExecutionMessages.add(systemMessage);
        currentExecutionMessages.add(userMessage);

        ChatRequest request = ChatRequest.builder()
                .messages(currentExecutionMessages)
                // We deliberately omit .toolSpecifications(toolSpecifications) here
                // to force a standard text generation from local models like Qwen.
                .build();

        ChatResponse response = chatModel.chat(request);
        AiMessage aiMessage = response.aiMessage();
        
        // Add response to memory
        currentExecutionMessages.add(aiMessage);
        
        return aiMessage.text();
    }

    /**
     * Phase 2: Execution
     * Executes the approved plan using a manual loop, handling tool calls dynamically.
     * 
     * @param originalPrompt The user's original task description
     * @param approvedPlan The step-by-step plan generated in Phase 1
     * @return The final confirmation string
     */
    public String executePlan(String originalPrompt, String approvedPlan) {
        clearMemory();
        
        // Ensure execution phase doesn't get confused by the LLMs natural language thoughts
        // We strip the <thought>...</thought> block entirely before feeding it back
        String cleanedPlan = approvedPlan.replaceAll("(?s)<thought>.*?</thought>\\s*", "").trim();
        
        SystemMessage systemMessage = SystemMessage.from("""
                You are an execution agent. Your job is to execute the following approved plan step by step using the tools provided.
                Execute the tools one by one as required. When you have finished all steps, give a brief final summary.
                """);
                
        UserMessage userMessage = UserMessage.from(
                "Original Goal: " + originalPrompt + "\n\n" +
                "Approved Plan to execute:\n" + cleanedPlan
        );
        
        currentExecutionMessages.add(systemMessage);
        currentExecutionMessages.add(userMessage);

        StringBuilder executionLog = new StringBuilder("Started Execution:\n");

        while (true) {
            ChatRequest request = ChatRequest.builder()
                    .messages(currentExecutionMessages)
                    .toolSpecifications(toolSpecifications)
                    .build();

            ChatResponse response = chatModel.chat(request);
            AiMessage aiMessage = response.aiMessage();
            currentExecutionMessages.add(aiMessage);

            if (aiMessage.hasToolExecutionRequests()) {
                // Execute all tools requested by the LLM
                for (ToolExecutionRequest toolRequest : aiMessage.toolExecutionRequests()) {
                    String toolName = toolRequest.name();
                    ToolExecutor executor = toolExecutors.get(toolName);
                    
                    if (executor != null) {
                        try {
                            // Execute the tool
                            String result = executor.execute(toolRequest, null);
                            
                            // Log it
                            executionLog.append("Tool called: ").append(toolName).append("\n");
                            executionLog.append("Result: ").append(result).append("\n\n");
                            
                            // Feed result back to memory
                            ToolExecutionResultMessage toolResultMessage = ToolExecutionResultMessage.from(
                                    toolRequest, result);
                            currentExecutionMessages.add(toolResultMessage);
                            
                        } catch (Exception e) {
                            String errorResult = "Error executing tool " + toolName + ": " + e.getMessage();
                            executionLog.append(errorResult).append("\n\n");
                            currentExecutionMessages.add(ToolExecutionResultMessage.from(toolRequest, errorResult));
                        }
                    } else {
                        String errorResult = "Tool executor not found for: " + toolName;
                        executionLog.append(errorResult).append("\n\n");
                        currentExecutionMessages.add(ToolExecutionResultMessage.from(toolRequest, errorResult));
                    }
                }
                // The loop continues, sending the ToolExecutionResultMessages back to the LLM
            } else {
                // No tools requested, LLM is done and giving final text
                executionLog.append("Agent Summary:\n").append(aiMessage.text());
                break;
            }
        }

        return executionLog.toString();
    }
}
