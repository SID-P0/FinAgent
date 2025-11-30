package com.org.pp.finAgent;

import com.org.pp.finAgent.controller.OCRController;
import com.org.pp.finAgent.service.AgentService;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class JavaFxApplication extends Application {
    private ConfigurableApplicationContext applicationContext;
    private OCRController ocrController;
    private AgentService agentService;

    @Override
    public void init() {
        // Bootstrap the Spring Boot application context
        applicationContext = new SpringApplicationBuilder(FinAgentApplication.class).run();
        // Get the service beans from the context
        this.ocrController = applicationContext.getBean(OCRController.class);
        this.agentService = applicationContext.getBean(AgentService.class);
    }

    @Override
    public void start(Stage stage) {
        // --- UI Components ---
        Label llmPromptLabel = new Label("LLM Command:");
        TextField llmPromptField = new TextField();
        llmPromptField.setPromptText("e.g., 'Find and click the text invoice'");
        Button llmSubmitButton = new Button("Submit to LLM");

        // --- New UI Components for OCR ---
        Label ocrLabel = new Label("Text to Find on Screen:");
        TextField ocrField = new TextField();
        ocrField.setPromptText("e.g., 'invoice'");
        Button ocrCtrlClickButton = new Button("Find & Ctrl+Click All");

        Label responseLabel = new Label("Response / Status:");
        TextArea responseArea = new TextArea();
        responseArea.setEditable(false);
        responseArea.setWrapText(true);

        // --- Layout ---
        VBox root = new VBox(10,
                llmPromptLabel, llmPromptField, llmSubmitButton,
                new Separator(), // Visually separate the two actions
                ocrLabel, ocrField, ocrCtrlClickButton,
                new Separator(),
                responseLabel, responseArea
        );
        root.setPadding(new Insets(15));

        // --- Event Handling ---
        llmSubmitButton.setDefaultButton(true);

        llmSubmitButton.setOnAction(event -> handleLlmAction(llmPromptField, responseArea, llmSubmitButton, ocrCtrlClickButton));
        ocrCtrlClickButton.setOnAction(event -> handleOcrAction(ocrField, responseArea, llmSubmitButton, ocrCtrlClickButton));

        // --- Scene and Stage Setup ---
        Scene scene = new Scene(root, 600, 450);
        stage.setTitle("FinAgent Control Panel");
        stage.setScene(scene);
        stage.show();
    }

    private void handleLlmAction(TextField promptField, TextArea responseArea, Button... buttonsToDisable) {
        String prompt = promptField.getText();
        if (prompt == null || prompt.isBlank()) {
            responseArea.setText("Please enter an LLM command.");
            return;
        }

        setButtonsDisabled(true, buttonsToDisable);
        responseArea.setText("Processing LLM command...");

        new Thread(() -> {
            try {
                final String responseText = agentService.chatWithAgent(prompt);

                Platform.runLater(() -> {
                    responseArea.setText(responseText);
                    setButtonsDisabled(false, buttonsToDisable);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    String errorMessage = "Error during LLM processing: " + e.getMessage();
                    if (e.getCause() != null) {
                        errorMessage += "\nCause: " + e.getCause().getMessage();
                    }
                    responseArea.setText(errorMessage);
                    setButtonsDisabled(false, buttonsToDisable);
                });
            }
        }).start();
    }

    private void handleOcrAction(TextField ocrField, TextArea responseArea, Button... buttonsToDisable) {
        String textToFind = ocrField.getText();
        if (textToFind == null || textToFind.isBlank()) {
            responseArea.setText("Please enter text to find for the OCR action.");
            return;
        }

        setButtonsDisabled(true, buttonsToDisable);
        responseArea.setText("Searching screen for '" + textToFind + "'...");

        new Thread(() -> {
            try {
                final int clickCount = ocrController.findAndCtrlClickAllByTextAndColor(textToFind, "#99c3ff");

                Platform.runLater(() -> {
                    responseArea.setText("Found and Ctrl+Clicked " + clickCount + " instance(s) of '" + textToFind + "'.");
                    setButtonsDisabled(false, buttonsToDisable);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    responseArea.setText("Error during OCR operation: " + e.getMessage());
                    setButtonsDisabled(false, buttonsToDisable);
                });
            }
        }).start();
    }

    private void setButtonsDisabled(boolean disabled, Button... buttons) {
        for (Button button : buttons) {
            button.setDisable(disabled);
        }
    }

    @Override
    public void stop() {
        applicationContext.close();
        Platform.exit();
    }
}