package com.org.pp.finAgent;

import com.org.pp.finAgent.service.AgentService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {
    private ConfigurableApplicationContext applicationContext;
    private AgentService agentService;
    private String currentPlan = null;
    private String currentPrompt = null;

    @Override
    public void init() {
        // Bootstrap the Spring Boot application context
        applicationContext = new SpringApplicationBuilder(FinAgentApplication.class).run();
        // Get the service beans from the context
        this.agentService = applicationContext.getBean(AgentService.class);
    }

    @Override
    public void start(Stage stage) {
        // --- UI Components ---
        Label llmPromptLabel = new Label("LLM Command:");
        TextField llmPromptField = new TextField();
        llmPromptField.setPromptText("e.g., 'Open chrome and click on blue links'");

        Button generatePlanButton = new Button("Generate Plan");
        Button executePlanButton = new Button("Execute Plan");
        executePlanButton.setDisable(true); // Initially disabled until a plan is generated

        HBox buttonBox = new HBox(10, generatePlanButton, executePlanButton);

        Label planLabel = new Label("Execution Plan:");
        TextArea planArea = new TextArea();
        planArea.setEditable(false);
        planArea.setWrapText(true);
        planArea.setPrefHeight(150);
        planArea.setPromptText("Plan will appear here after clicking 'Generate Plan'");

        Label responseLabel = new Label("Response / Status:");
        TextArea responseArea = new TextArea();
        responseArea.setEditable(false);
        responseArea.setWrapText(true);
        responseArea.setPrefHeight(150);

        // --- Layout ---
        VBox root = new VBox(10,
                llmPromptLabel, llmPromptField, buttonBox,
                planLabel, planArea,
                responseLabel, responseArea);
        root.setPadding(new Insets(15));

        // --- Event Handling ---
        generatePlanButton.setOnAction(event -> handleGeneratePlan(
                llmPromptField, planArea, responseArea, generatePlanButton, executePlanButton));

        executePlanButton.setOnAction(event -> handleExecutePlan(
                planArea, responseArea, generatePlanButton, executePlanButton));

        // --- Scene and Stage Setup ---
        Scene scene = new Scene(root, 650, 550);
        stage.setTitle("FinAgent Control Panel");
        stage.setScene(scene);
        stage.show();
    }

    private void handleGeneratePlan(TextField promptField, TextArea planArea, TextArea responseArea,
            Button generateButton, Button executeButton) {
        String prompt = promptField.getText();
        if (prompt == null || prompt.isBlank()) {
            responseArea.setText("Please enter an LLM command.");
            return;
        }

        generateButton.setDisable(true);
        executeButton.setDisable(true);
        planArea.setText("Generating plan...");
        responseArea.setText("Waiting for plan generation...");

        currentPrompt = prompt;

        new Thread(() -> {
            try {
                // Clear memory before generating a new plan to start fresh
                agentService.clearMemory();
                // Create a planning prompt that asks the LLM to break down the task
                String planningPrompt = "Create a step-by-step execution plan for the following task. " +
                        "List each step clearly and concisely. Do not execute anything yet, just plan:\n\n" +
                        "Task: " + prompt + "\n\n" +
                        "Provide a numbered list of steps you would take to accomplish this task.";

                final String plan = agentService.chatWithAgent(planningPrompt);
                currentPlan = plan;

                Platform.runLater(() -> {
                    planArea.setText(plan);
                    responseArea.setText("Plan generated successfully. Click 'Execute Plan' to proceed.");
                    generateButton.setDisable(false);
                    executeButton.setDisable(false); // Enable execute button once plan is ready
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    String errorMessage = "Error generating plan: " + e.getMessage();
                    if (e.getCause() != null) {
                        errorMessage += "\nCause: " + e.getCause().getMessage();
                    }
                    planArea.setText(errorMessage);
                    responseArea.setText("Failed to generate plan. Please try again.");
                    generateButton.setDisable(false);
                    executeButton.setDisable(true);
                });
            }
        }).start();
    }

    private void handleExecutePlan(TextArea planArea, TextArea responseArea,
            Button generateButton, Button executeButton) {
        if (currentPrompt == null || currentPlan == null) {
            responseArea.setText("No plan available. Please generate a plan first.");
            return;
        }

        generateButton.setDisable(true);
        executeButton.setDisable(true);
        responseArea.setText("Executing plan...");

        new Thread(() -> {
            try {
                /**
                 * Prompt validation
                 * Task planner (Decide which methods should be addded in the plan and ask to
                 * execute them)
                 * Example: Open google chrome and search for 'Wordle'
                 * 1. Open the google chrome application (There could be various ways to open an
                 * app from
                 * desktop, clicking the icon, finding it via start menu, finding it on task
                 * bar) (Langchain Tool to index out all applications and open them)
                 * 2. Make the mouse move to the search bar on chrome and search for wordle
                 * 3. Click on the website for wordle
                 * 
                 * For step 2 and 3 we need LLM to plan out
                 */

                // Common steps to carry out after every step in the planner
                // 1. Ocr ( What do we see on the screen ? Are we at the desired screen ?)
                // 2. If not try to do chanegs in plan for alternate route on how to reach for
                // desired screen ?
                // 3. Move the mouse corresponding to the LLM response from step 2 (Open, Click,
                // Type)

                // Execute the original prompt with the agent
                final String responseText = agentService.chatWithAgent(currentPrompt);

                Platform.runLater(() -> {
                    responseArea.setText("Execution Complete!\n\n" + responseText);
                    generateButton.setDisable(false);
                    executeButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    String errorMessage = "Error during execution: " + e.getMessage();
                    if (e.getCause() != null) {
                        errorMessage += "\nCause: " + e.getCause().getMessage();
                    }
                    responseArea.setText(errorMessage);
                    generateButton.setDisable(false);
                    executeButton.setDisable(false);
                });
            }
        }).start();
    }

    @Override
    public void stop() {
        applicationContext.close();
        Platform.exit();
    }
}