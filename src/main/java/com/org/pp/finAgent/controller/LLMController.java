package com.org.pp.finAgent.controller;

import com.google.genai.Client;
import com.google.genai.types.*;
import com.org.pp.finAgent.util.ScreenCapture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class LLMController {

    private static final Logger LOGGER = Logger.getLogger(LLMController.class.getName());

    private final String TEXT_MODEL_NAME = "gemini-2.5-flash";
    private final String VIDEO_MODEL_NAME = "gemini-2.5-flash";

    @Autowired
    private Client client;

    /**
     * Analyzes the current screen with a user's instruction to generate a structured mouse command.
     * This method uses the model's JSON mode for reliable, parsable output.
     *
     * @param userInstruction The high-level user command (e.g., "Click on the 'Submit' button").
     * @return A JSON string representing the mouse command, ready to be parsed.
     * @throws IOException  If there is an issue with screen capture or API communication.
     * @throws AWTException If there is an issue with screen capture.
     */
    public GenerateContentResponse getMouseCommandFromScreen(String userInstruction) throws IOException, AWTException {
        // 1. Define the JSON schema you want the model to return.
        // This is the "contract" between the LLM and your MouseMovement class.
        String jsonSchema = "{\"action\": \"string\", \"x\": \"integer\", \"y\": \"integer\", \"button\": \"string\", \"amount\": \"integer\"}";

        // 2. Construct a detailed prompt that instructs the model on its task.
        // This is the most critical part for getting reliable results.
        String systemPrompt = String.format(
                "You are an expert desktop automation assistant. Your task is to analyze a screenshot of a computer screen " +
                        "and a user's instruction. Based on this, you must identify the correct coordinates and action to perform. " +
                        "You MUST respond with ONLY a single, valid JSON object that conforms to the following schema: %s. " +
                        "\n\n" +
                        "Here are the possible actions:\n" +
                        "- 'MOVE': Move the mouse to the specified x, y coordinates.\n" +
                        "- 'CLICK': Click the specified button ('LEFT', 'RIGHT', 'MIDDLE') at the CURRENT mouse location. 'x' and 'y' can be null.\n" +
                        "- 'MOVE_AND_CLICK': Move the mouse to x, y and then click the specified button.\n" +
                        "- 'SCROLL': Scroll the mouse wheel by the specified 'amount'. Positive is down, negative is up. 'x' and 'y' can be null.\n" +
                        "\n" +
                        "Analyze the user's instruction and the provided image carefully to determine the target and the appropriate action. " +
                        "If the user says 'click on the file menu', find the coordinates of the 'file menu' and generate a 'MOVE_AND_CLICK' action.",
                jsonSchema
        );

        // 3. Capture the screen and build the request parts.
        Part imagePart = Part.builder()
                .inlineData(Blob.builder()
                        .data(ScreenCapture.captureAsBytes())
                        .mimeType("image/png")
                        .build())
                .build();

        // Combine the system prompt and the specific user instruction into the text part.
        Part textPart = Part.builder()
                .text(systemPrompt + "\n\nUSER INSTRUCTION: \"" + userInstruction + "\"")
                .build();

        Content content = Content.builder()
                .parts(List.of(imagePart, textPart))
                .build();

        // 4. Configure the model to use JSON output mode.
        // This forces the model to return a valid JSON object.
        GenerateContentConfig generationConfig = GenerateContentConfig.builder().responseMimeType("application/json").build();


        // 5. Call the model and extract the JSON response.
        try {
            return client.models.generateContent(VIDEO_MODEL_NAME,content,generationConfig);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to get command from LLM. The model may have returned an empty or invalid response.", e);
            // Return a "no-op" JSON or throw a custom exception
            return null;
        }
    }

    /**
     * A "planner" or "router" method that first asks the LLM if a screenshot is needed to answer the prompt.
     * This saves resources by not performing a screen capture and vision analysis for simple questions.
     *
     * @param prompt The user's question or command.
     * @return The model's final response, either a JSON command string or a natural language answer.
     */
    public GenerateContentResponse processUserPrompt(String prompt) {
        try {
            if (isScreenAnalysisRequired(prompt)) {
                LOGGER.info("Screen analysis is required. Capturing screen and generating command...");
                // This will return the JSON command string
                return getMouseCommandFromScreen(prompt);
            } else {
                LOGGER.info("Screen analysis not required. Answering directly...");
                // For a simple text answer, we don't need JSON mode.
                // This will return a natural language answer
                return client.models.generateContent(TEXT_MODEL_NAME, prompt, null);
            }
        } catch (IOException | AWTException e) {
            LOGGER.log(Level.SEVERE, "An error occurred during prompt processing.", e);
            return null;
        }
    }

    /**
     * Asks the model a simple yes/no question to determine if a screenshot is needed.
     *
     * @param prompt The user's original prompt.
     * @return true if the model indicates a screen analysis is needed, false otherwise.
     */
    private boolean isScreenAnalysisRequired(String prompt) {
        String decisionPrompt = "You are a helpful assistant. A user has given the following instruction: \"" + prompt + "\". " +
                "Based on this instruction, If there is anything related to screen/monitor/desktop applications just return 'True' or else return 'False'" +
                "Answer with only the word 'True' or 'False'.";

        try {
            GenerateContentResponse response = client.models.generateContent(TEXT_MODEL_NAME, decisionPrompt, null);
            String decision = getTextFromResponse(response);
            LOGGER.info("Model's decision on needing a screenshot: " + decision);
            return "True".equals(decision);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not determine if screen analysis is required. Defaulting to false.", e);
            return false; // Default to not requiring a screenshot on error
        }
    }

    /**
     * Safely extracts the text content from a GenerateContentResponse.
     *
     * @param response The response from the Gemini API.
     * @return The extracted text, or an empty string if not found.
     */
    private String getTextFromResponse(GenerateContentResponse response) {
        if (response != null && response.candidates().isPresent()) {
            Candidate candidate = response.candidates().get().getFirst();
            if (candidate.content() != null && candidate.content().isPresent()) {
                return candidate.content().get().text();
            }
        }
        LOGGER.warning("No text content found in the LLM response.");
        return "";
    }
}