package com.org.pp.finAgent.automation;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.springframework.stereotype.Service;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class KeyboardMovement {

    private static final Logger LOGGER = Logger.getLogger(KeyboardMovement.class.getName());
    private final Robot robot;
    private final Gson gson = new Gson();
    private final boolean isMacOS;
    private final Map<String, Integer> keyCodeMap = new HashMap<>();

    // A simple data class to hold the deserialized command
    private static class KeyboardCommand {
        String action;
        String text;
        String key;
        String[] keys; // For key combinations
    }

    public KeyboardMovement() throws AWTException {
        this.robot = new Robot();
        this.robot.setAutoDelay(50); // A small delay between robot events
        this.isMacOS = System.getProperty("os.name").toLowerCase().contains("mac");
        initializeKeyCodeMap();
        LOGGER.info("KeyboardMovement initialized. Detected OS: " + (isMacOS ? "macOS" : "Windows/Linux"));
    }

    private void initializeKeyCodeMap() {
        // Letters
        for (char c = 'A'; c <= 'Z'; c++) {
            keyCodeMap.put(String.valueOf(c), KeyEvent.VK_A + (c - 'A'));
        }

        // Numbers
        for (int i = 0; i <= 9; i++) {
            keyCodeMap.put(String.valueOf(i), KeyEvent.VK_0 + i);
        }

        // Special keys
        keyCodeMap.put("ENTER", KeyEvent.VK_ENTER);
        keyCodeMap.put("TAB", KeyEvent.VK_TAB);
        keyCodeMap.put("ESCAPE", KeyEvent.VK_ESCAPE);
        keyCodeMap.put("ESC", KeyEvent.VK_ESCAPE);
        keyCodeMap.put("BACKSPACE", KeyEvent.VK_BACK_SPACE);
        keyCodeMap.put("DELETE", KeyEvent.VK_DELETE);
        keyCodeMap.put("SPACE", KeyEvent.VK_SPACE);

        // Arrow keys
        keyCodeMap.put("UP", KeyEvent.VK_UP);
        keyCodeMap.put("DOWN", KeyEvent.VK_DOWN);
        keyCodeMap.put("LEFT", KeyEvent.VK_LEFT);
        keyCodeMap.put("RIGHT", KeyEvent.VK_RIGHT);

        // Modifier keys
        keyCodeMap.put("CTRL", KeyEvent.VK_CONTROL);
        keyCodeMap.put("CONTROL", KeyEvent.VK_CONTROL);
        keyCodeMap.put("ALT", KeyEvent.VK_ALT);
        keyCodeMap.put("SHIFT", KeyEvent.VK_SHIFT);
        keyCodeMap.put("CMD", isMacOS ? KeyEvent.VK_META : KeyEvent.VK_CONTROL);
        keyCodeMap.put("COMMAND", isMacOS ? KeyEvent.VK_META : KeyEvent.VK_CONTROL);
        keyCodeMap.put("META", KeyEvent.VK_META);

        // Function keys
        for (int i = 1; i <= 12; i++) {
            keyCodeMap.put("F" + i, KeyEvent.VK_F1 + (i - 1));
        }

        // Other common keys
        keyCodeMap.put("HOME", KeyEvent.VK_HOME);
        keyCodeMap.put("END", KeyEvent.VK_END);
        keyCodeMap.put("PAGE_UP", KeyEvent.VK_PAGE_UP);
        keyCodeMap.put("PAGE_DOWN", KeyEvent.VK_PAGE_DOWN);
    }

    public void executeCommand(String jsonCommand) {
        try {
            KeyboardCommand command = gson.fromJson(jsonCommand, KeyboardCommand.class);
            if (command == null || command.action == null) {
                LOGGER.warning("Invalid or empty command received: " + jsonCommand);
                return;
            }

            LOGGER.info("Executing keyboard action: " + command.action);

            switch (command.action.toUpperCase()) {
                case "TYPE_TEXT":
                    if (command.text != null) {
                        typeText(command.text);
                    }
                    break;

                case "PRESS_KEY":
                    if (command.key != null) {
                        pressKey(command.key);
                    }
                    break;

                case "PRESS_KEY_COMBINATION":
                    if (command.keys != null && command.keys.length > 0) {
                        pressKeyCombination(command.keys);
                    }
                    break;

                default:
                    LOGGER.warning("Unknown action: " + command.action);
                    break;
            }
        } catch (JsonSyntaxException e) {
            LOGGER.log(Level.SEVERE, "Failed to parse JSON command: " + jsonCommand, e);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred during keyboard command execution.", e);
        }
    }

    public void typeText(String text) {
        if (text == null || text.isEmpty()) {
            LOGGER.warning("Cannot type empty text");
            return;
        }

        LOGGER.info("Typing text: " + text);

        for (char c : text.toCharArray()) {
            typeCharacter(c);
        }
    }

    private void typeCharacter(char c) {
        boolean needsShift = Character.isUpperCase(c) || isShiftRequired(c);
        int keyCode = getKeyCodeForCharacter(c);

        if (keyCode != -1) {
            if (needsShift) {
                robot.keyPress(KeyEvent.VK_SHIFT);
            }

            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);

            if (needsShift) {
                robot.keyRelease(KeyEvent.VK_SHIFT);
            }
        } else {
            LOGGER.warning("Cannot type character: " + c);
        }
    }

    private boolean isShiftRequired(char c) {
        // Characters that require shift key
        return "!@#$%^&*()_+{}|:\"<>?".indexOf(c) != -1;
    }

    private int getKeyCodeForCharacter(char c) {
        // Handle special shift characters
        Map<Character, Integer> shiftCharMap = new HashMap<>();
        shiftCharMap.put('!', KeyEvent.VK_1);
        shiftCharMap.put('@', KeyEvent.VK_2);
        shiftCharMap.put('#', KeyEvent.VK_3);
        shiftCharMap.put('$', KeyEvent.VK_4);
        shiftCharMap.put('%', KeyEvent.VK_5);
        shiftCharMap.put('^', KeyEvent.VK_6);
        shiftCharMap.put('&', KeyEvent.VK_7);
        shiftCharMap.put('*', KeyEvent.VK_8);
        shiftCharMap.put('(', KeyEvent.VK_9);
        shiftCharMap.put(')', KeyEvent.VK_0);
        shiftCharMap.put('_', KeyEvent.VK_MINUS);
        shiftCharMap.put('+', KeyEvent.VK_EQUALS);
        shiftCharMap.put('{', KeyEvent.VK_OPEN_BRACKET);
        shiftCharMap.put('}', KeyEvent.VK_CLOSE_BRACKET);
        shiftCharMap.put('|', KeyEvent.VK_BACK_SLASH);
        shiftCharMap.put(':', KeyEvent.VK_SEMICOLON);
        shiftCharMap.put('"', KeyEvent.VK_QUOTE);
        shiftCharMap.put('<', KeyEvent.VK_COMMA);
        shiftCharMap.put('>', KeyEvent.VK_PERIOD);
        shiftCharMap.put('?', KeyEvent.VK_SLASH);

        if (shiftCharMap.containsKey(c)) {
            return shiftCharMap.get(c);
        }

        // Handle uppercase letters
        if (Character.isUpperCase(c)) {
            return KeyEvent.VK_A + (c - 'A');
        }

        // Handle lowercase letters
        if (Character.isLowerCase(c)) {
            return KeyEvent.VK_A + (Character.toUpperCase(c) - 'A');
        }

        // Handle digits
        if (Character.isDigit(c)) {
            return KeyEvent.VK_0 + (c - '0');
        }

        // Handle other common characters
        switch (c) {
            case ' ':
                return KeyEvent.VK_SPACE;
            case '-':
                return KeyEvent.VK_MINUS;
            case '=':
                return KeyEvent.VK_EQUALS;
            case '[':
                return KeyEvent.VK_OPEN_BRACKET;
            case ']':
                return KeyEvent.VK_CLOSE_BRACKET;
            case '\\':
                return KeyEvent.VK_BACK_SLASH;
            case ';':
                return KeyEvent.VK_SEMICOLON;
            case '\'':
                return KeyEvent.VK_QUOTE;
            case ',':
                return KeyEvent.VK_COMMA;
            case '.':
                return KeyEvent.VK_PERIOD;
            case '/':
                return KeyEvent.VK_SLASH;
            case '`':
                return KeyEvent.VK_BACK_QUOTE;
            default:
                return -1;
        }
    }

    public void pressKey(String key) {
        if (key == null || key.isEmpty()) {
            LOGGER.warning("Cannot press empty key");
            return;
        }

        Integer keyCode = keyCodeMap.get(key.toUpperCase());
        if (keyCode != null) {
            LOGGER.info("Pressing key: " + key);
            robot.keyPress(keyCode);
            robot.keyRelease(keyCode);
        } else {
            LOGGER.warning("Unknown key: " + key);
        }
    }

    public void pressKeyCombination(String[] keys) {
        if (keys == null || keys.length == 0) {
            LOGGER.warning("Cannot press empty key combination");
            return;
        }

        LOGGER.info("Pressing key combination: " + String.join("+", keys));

        // Press all keys in order
        int[] keyCodes = new int[keys.length];
        for (int i = 0; i < keys.length; i++) {
            Integer keyCode = keyCodeMap.get(keys[i].toUpperCase());
            if (keyCode != null) {
                keyCodes[i] = keyCode;
                robot.keyPress(keyCode);
            } else {
                LOGGER.warning("Unknown key in combination: " + keys[i]);
                // Release any keys we've already pressed
                for (int j = 0; j < i; j++) {
                    robot.keyRelease(keyCodes[j]);
                }
                return;
            }
        }

        // Release all keys in reverse order
        for (int i = keys.length - 1; i >= 0; i--) {
            robot.keyRelease(keyCodes[i]);
        }
    }
}
