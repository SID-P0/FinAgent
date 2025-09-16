package com.org.pp.finAgent;

import com.google.genai.types.GenerateContentResponse;
import com.org.pp.finAgent.controller.LLMController;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext applicationContext;
    private LLMController llmService;

    @Override
    public void init() {
        // Bootstrap the Spring Boot application context
        applicationContext = new SpringApplicationBuilder(FinAgentApplication.class).run();
        // Get the LLMService bean from the context
        this.llmService = applicationContext.getBean(LLMController.class);
    }

    @Override
    public void start(Stage stage) {
        // --- UI Components ---
        Label promptLabel = new Label("Enter your command:");
        TextField promptField = new TextField();
        promptField.setPromptText("e.g., 'What is the capital of France?'");

        Button submitButton = new Button("Submit");

        Label responseLabel = new Label("Response:");
        TextArea responseArea = new TextArea();
        responseArea.setEditable(false);
        responseArea.setWrapText(true);

        // --- Layout ---
        VBox root = new VBox(10, promptLabel, promptField, submitButton, responseLabel, responseArea);
        root.setPadding(new Insets(15));

        // --- Event Handling ---
        // By setting this button as the default, pressing "Enter" will trigger it.
        submitButton.setDefaultButton(true);

        submitButton.setOnAction(event -> {
            String prompt = promptField.getText();
            if (prompt == null || prompt.isBlank()) {
                responseArea.setText("Please enter a prompt.");
                return;
            }

            // Disable button to prevent multiple clicks
            submitButton.setDisable(true);
            responseArea.setText("Generating response...");

            // Run the network call on a background thread
            new Thread(() -> {
                try {
                    // CORRECTED: Use the text-only method for this UI
                    GenerateContentResponse response = llmService.analyzeScreen(prompt);
                    final String responseText = response.text();

                    // Update the UI on the JavaFX Application Thread
                    Platform.runLater(() -> {
                        responseArea.setText(responseText);
                        submitButton.setDisable(false);
                    });

                } catch (Exception e) {
                    // Handle errors and update the UI
                    Platform.runLater(() -> {
                        responseArea.setText("Error: " + e.getMessage());
                        submitButton.setDisable(false);
                    });
                }
            }).start();
        });

        // --- Scene and Stage Setup ---
        Scene scene = new Scene(root, 600, 400);
        stage.setTitle("BridgeConnect");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        // Cleanly shut down the Spring application context
        applicationContext.close();
        Platform.exit();
    }
}