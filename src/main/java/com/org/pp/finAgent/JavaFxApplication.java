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

        // --- Button for clicking all blue links ---
        Button clickAllBlueLinksButton = new Button("Click All Blue Links");

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
                clickAllBlueLinksButton,
                new Separator(),
                responseLabel, responseArea);
        root.setPadding(new Insets(15));

        // --- Event Handling ---
        llmSubmitButton.setDefaultButton(true);

        llmSubmitButton.setOnAction(
                event -> handleLlmAction(llmPromptField, responseArea, llmSubmitButton, ocrCtrlClickButton,
                        clickAllBlueLinksButton));
        ocrCtrlClickButton
                .setOnAction(event -> handleOcrAction(ocrField, responseArea, llmSubmitButton, ocrCtrlClickButton,
                        clickAllBlueLinksButton));
        clickAllBlueLinksButton
                .setOnAction(event -> handleClickAllBlueLinksAction(responseArea, llmSubmitButton, ocrCtrlClickButton,
                        clickAllBlueLinksButton));

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
                    responseArea
                            .setText("Found and Ctrl+Clicked " + clickCount + " instance(s) of '" + textToFind + "'.");
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

    private void handleClickAllBlueLinksAction(TextArea responseArea, Button... buttonsToDisable) {
        setButtonsDisabled(true, buttonsToDisable);
        responseArea.setText("Searching screen for all blue links...");

        // Blue link color - typical blue hyperlink color from the screenshot
        final String BLUE_LINK_COLOR = "#5A9CFD";

        new Thread(() -> {
            try {
                final int clickCount = ocrController.openAllGoogleSearchLinks(BLUE_LINK_COLOR);

                Platform.runLater(() -> {
                    responseArea.setText("Found and Ctrl+Clicked " + clickCount + " blue link(s).");
                    setButtonsDisabled(false, buttonsToDisable);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    responseArea.setText("Error during blue links detection: " + e.getMessage());
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