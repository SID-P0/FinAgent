package com.org.pp.finAgent.controller;


import com.google.genai.Client;
import com.google.genai.types.*;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.protobuf.ByteString;
import com.org.pp.finAgent.util.ScreenCapture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
public class LLMController {
    @Autowired
    private Client client;

    /**
     * Analyzes an image (like a screenshot) with a textual prompt.
     *
     * @param prompt The text prompt to send to the model (e.g., "What is on the screen?").
     * @return The model's generated response.
     * @throws IOException If there is an issue communicating with the model.
     */
    public GenerateContentResponse analyzeScreen(String prompt) throws IOException, AWTException {
        String modelName = "gemini-2.5-flash";

        // Build the request content with both a text part and an image part.
        Part imagePart = Part.builder()
                .inlineData(
                        Blob.builder()
                                .data(ScreenCapture.captureAsBytes())
                                .mimeType("image/png")
                                .build())
                .build();
        Part textPart = Part.builder()
                .text(prompt).build();
        List<Part> allParts = new ArrayList<>();
        allParts.add(imagePart);
        allParts.add(textPart);

        Content content = Content.builder().parts(allParts).build();

        return client.models.generateContent(modelName, content, null);
    }

    public GenerateContentResponse getInputCommands(String prompt) {
        // TODO Add it such that the prompt will always return a true or false value
        //  whether it needs the screenshot of the system or not based on the question
        //  if yes than call analyze screen method or just answer the user
//        if(flag){
//            try {
//                analyzeScreen(prompt);
//            } catch (IOException | AWTException e) {
//                throw new RuntimeException(e);
//            }
//        }
        return client.models.generateContent("gemini-2.5-flash", prompt, null);
    }

}
