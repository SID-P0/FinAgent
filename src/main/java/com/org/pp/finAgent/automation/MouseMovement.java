package com.org.pp.finAgent.automation;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.springframework.stereotype.Service;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent; // Required for keyboard keys
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class MouseMovement {

    private static final Logger LOGGER = Logger.getLogger(MouseMovement.class.getName());
    private final Robot robot;
    private final Gson gson = new Gson();

    // A simple data class to hold the deserialized command
    private static class MouseCommand {
        String action;
        Integer x;
        Integer y;
        String button; // "LEFT", "RIGHT", "MIDDLE"
    }

    public MouseMovement() throws AWTException {
        this.robot = new Robot();
        this.robot.setAutoDelay(50); // A small delay between robot events is good practice
    }

    public void executeCommand(String jsonCommand) {
        try {
            MouseCommand command = gson.fromJson(jsonCommand, MouseCommand.class);
            if (command == null || command.action == null) {
                LOGGER.warning("Invalid or empty command received: " + jsonCommand);
                return;
            }

            LOGGER.info("Executing action: " + command.action);

            switch (command.action.toUpperCase()) {
                case "MOVE_AND_CLICK":
                    if (isValidCoordinates(command.x, command.y) && command.button != null) {
                        robot.mouseMove(command.x, command.y);
                        performClick(command.button);
                    }
                    break;

                // New action for Ctrl+Click
                case "MOVE_AND_CTRL_CLICK":
                    if (isValidCoordinates(command.x, command.y) && command.button != null) {
                        robot.mouseMove(command.x, command.y);
                        performCtrlClick(command.button);
                    }
                    break;

                default:
                    LOGGER.warning("Unknown action: " + command.action);
                    break;
            }
        } catch (JsonSyntaxException e) {
            LOGGER.log(Level.SEVERE, "Failed to parse JSON command: " + jsonCommand, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred during command execution.", e);
        }
    }

    private void performClick(String button) {
        int buttonMask = getButtonMask(button);
        if (buttonMask != -1) {
            robot.mousePress(buttonMask);
            robot.mouseRelease(buttonMask);
        }
    }

    // New helper method to perform a Ctrl+Click
    private void performCtrlClick(String button) {
        int buttonMask = getButtonMask(button);
        if (buttonMask != -1) {
            robot.keyPress(KeyEvent.VK_CONTROL);   // Press and hold the Control key
            robot.mousePress(buttonMask);        // Press the mouse button
            robot.mouseRelease(buttonMask);      // Release the mouse button
            robot.keyRelease(KeyEvent.VK_CONTROL); // Release the Control key
        }
    }

    private int getButtonMask(String button) {
        switch (button.toUpperCase()) {
            case "LEFT":
                return InputEvent.BUTTON1_DOWN_MASK;
            case "RIGHT":
                return InputEvent.BUTTON3_DOWN_MASK;
            case "MIDDLE":
                return InputEvent.BUTTON2_DOWN_MASK;
            default:
                LOGGER.warning("Unknown button type: " + button);
                return -1;
        }
    }

    private boolean isValidCoordinates(Integer x, Integer y) {
        if (x == null || y == null) {
            LOGGER.warning("Coordinates cannot be null for a MOVE action.");
            return false;
        }
        return true;
    }
}