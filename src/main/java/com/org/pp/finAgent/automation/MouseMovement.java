package com.org.pp.finAgent.automation;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.springframework.stereotype.Service;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class MouseMovement {

    private static final Logger LOGGER = Logger.getLogger(MouseMovement.class.getName());
    private final Robot robot;
    private final Gson gson = new Gson();
    private final boolean isMacOS;

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
        this.isMacOS = System.getProperty("os.name").toLowerCase().contains("mac");
        LOGGER.info("MouseMovement initialized. Detected OS: " + (isMacOS ? "macOS" : "Windows/Linux"));
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

                // Cross-platform multi-select click (Ctrl on Windows/Linux, Command on macOS)
                case "MOVE_AND_CTRL_CLICK":
                    if (isValidCoordinates(command.x, command.y) && command.button != null) {
                        robot.mouseMove(command.x, command.y);
                        performModifierClick(command.button);
                    }
                    break;

                // Explicit Command+Click (macOS) - uses Ctrl on Windows
                case "MOVE_AND_CMD_CLICK":
                    if (isValidCoordinates(command.x, command.y) && command.button != null) {
                        robot.mouseMove(command.x, command.y);
                        performModifierClick(command.button);
                    }
                    break;

                // Right-click context menu (works the same on both platforms)
                case "MOVE_AND_RIGHT_CLICK":
                    if (isValidCoordinates(command.x, command.y)) {
                        robot.mouseMove(command.x, command.y);
                        performClick("RIGHT");
                    }
                    break;

                // Double-click action
                case "MOVE_AND_DOUBLE_CLICK":
                    if (isValidCoordinates(command.x, command.y) && command.button != null) {
                        robot.mouseMove(command.x, command.y);
                        performDoubleClick(command.button);
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

    private void performDoubleClick(String button) {
        int buttonMask = getButtonMask(button);
        if (buttonMask != -1) {
            robot.mousePress(buttonMask);
            robot.mouseRelease(buttonMask);
            robot.mousePress(buttonMask);
            robot.mouseRelease(buttonMask);
        }
    }

    /**
     * Performs a modifier+click action that is cross-platform compatible.
     * On macOS: Uses Command (⌘) key - this is the standard for multi-select
     * On Windows/Linux: Uses Control key
     */
    private void performModifierClick(String button) {
        int buttonMask = getButtonMask(button);
        if (buttonMask != -1) {
            int modifierKey = isMacOS ? KeyEvent.VK_META : KeyEvent.VK_CONTROL;
            robot.keyPress(modifierKey);
            robot.mousePress(buttonMask);
            robot.mouseRelease(buttonMask);
            robot.keyRelease(modifierKey);
        }
    }

    /**
     * Gets the appropriate button mask for the given button name.
     * Note: On macOS, right-click can also be simulated with Ctrl+Click,
     * but we use the standard button masks for consistency.
     */
    private int getButtonMask(String button) {
        switch (button.toUpperCase()) {
            case "LEFT":
                return InputEvent.BUTTON1_DOWN_MASK;
            case "RIGHT":
                // On macOS, BUTTON3 works correctly for right-click
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