package com.org.pp.finAgent.agent.tools;

import com.org.pp.finAgent.controller.OCRController;
import dev.langchain4j.agent.tool.Tool;

public class AgentTools {
    private final OCRController ocrController;

    public AgentTools(OCRController ocrController) {
        this.ocrController = ocrController;
    }

    @Tool("Finds the specified text on the screen and control-clicks on it")
    public String findAndClickText(String textToFind) {
        try {
            // Using a default color, as the agent won't know about color codes
            int count = ocrController.findAndCtrlClickAllByTextAndColor(textToFind, "#99c3ff");
            if (count > 0) {
                return "Successfully found and clicked " + count + " instance(s) of '" + textToFind + "'.";
            } else {
                return "Could not find any instances of '" + textToFind + "' on the screen.";
            }
        } catch (Exception e) {
            return "Error during OCR operation: " + e.getMessage();
        }
    }
}
