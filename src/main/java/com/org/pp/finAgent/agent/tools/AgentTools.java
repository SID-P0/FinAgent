package com.org.pp.finAgent.agent.tools;

import com.org.pp.finAgent.automation.KeyboardMovement;
import com.org.pp.finAgent.controller.OCRController;
import com.org.pp.finAgent.util.ApplicationScanner;
import dev.langchain4j.agent.tool.Tool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

@Component
public class AgentTools {
    private final KeyboardMovement keyboardMovement;
    private final ApplicationScanner applicationScanner;
    private final Map<String, String> applicationCache;
    private final boolean isWindows;
    private final boolean isMac;

    public AgentTools(KeyboardMovement keyboardMovement) {
        this.keyboardMovement = keyboardMovement;
        this.applicationScanner = new ApplicationScanner();
        String os = System.getProperty("os.name").toLowerCase();
        this.isWindows = os.contains("win");
        this.isMac = os.contains("mac");
        // Pre-populate the application cache
        this.applicationCache = applicationScanner.scanInstalledApplications();
    }

    /**
     * Waits for 1 second to allow the window to refresh after an action.
     */
    private void waitForWindowRefresh() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @Tool("Lists all installed applications on this machine. Returns a list of application names that can be launched.")
    public String listInstalledApplications() {
        try {
            // Refresh the cache
            Map<String, String> apps = applicationScanner.scanInstalledApplications();
            if (apps.isEmpty()) {
                return "No applications found.";
            }
            List<String> appNames = new ArrayList<>(apps.keySet());
            appNames.sort(String.CASE_INSENSITIVE_ORDER);
            return "Found " + appNames.size() + " applications:\n" + String.join(", ", appNames);
        } catch (Exception e) {
            return "Error listing applications: " + e.getMessage();
        }
    }

    @Tool("Launches an application by name. Use listInstalledApplications first to see available apps. The appName should match or partially match an installed application name.")
    public String launchApplication(String appName) {
        try {
            if (appName == null || appName.isBlank()) {
                return "Please provide an application name to launch.";
            }

            // Find matching application (case-insensitive partial match)
            String matchedApp = null;
            String matchedPath = null;
            String lowerAppName = appName.toLowerCase();

            for (Map.Entry<String, String> entry : applicationCache.entrySet()) {
                if (entry.getKey().toLowerCase().contains(lowerAppName)) {
                    matchedApp = entry.getKey();
                    matchedPath = entry.getValue();
                    break;
                }
            }

            if (matchedPath == null) {
                return "Application '" + appName + "' not found. Use listInstalledApplications to see available apps.";
            }

            // Launch the application
            ProcessBuilder pb;
            boolean isChrome = matchedApp.toLowerCase().contains("chrome");

            if (isWindows) {
                if (matchedPath.endsWith(".lnk")) {
                    // Launch shortcut using cmd
                    pb = new ProcessBuilder("cmd", "/c", "start", "", matchedPath);
                } else {
                    pb = new ProcessBuilder("cmd", "/c", "start", "", matchedPath);
                }
            } else if (isMac) {
                pb = new ProcessBuilder("open", matchedPath);
            } else {
                // Linux fallback
                pb = new ProcessBuilder("xdg-open", matchedPath);
            }

            pb.start();

            // Wait for the application to open
            waitForWindowRefresh();

            // If it's Chrome, open incognito window using Ctrl+Shift+N
            if (isChrome) {
                Thread.sleep(500); // Extra wait for Chrome to fully load
                keyboardMovement.pressKeyCombination(new String[] { isMac ? "CMD" : "CTRL", "SHIFT", "N" });
                waitForWindowRefresh();
                return "Successfully launched '" + matchedApp + "' and opened incognito window.";
            }

            return "Successfully launched '" + matchedApp + "'.";
        } catch (Exception e) {
            return "Error launching application: " + e.getMessage();
        }
    }

    @Tool("Types the specified text at the current cursor position. The text will be typed character by character.")
    public String typeText(String text) {
        try {
            if (text == null || text.isBlank()) {
                return "Please provide text to type.";
            }
            keyboardMovement.typeText(text);
            waitForWindowRefresh();
            return "Successfully typed: '" + text + "'";
        } catch (Exception e) {
            return "Error typing text: " + e.getMessage();
        }
    }

    @Tool("Presses a single key. Supports special keys like ENTER, TAB, ESCAPE, arrow keys (UP, DOWN, LEFT, RIGHT), function keys (F1-F12), etc.")
    public String pressKey(String key) {
        try {
            if (key == null || key.isBlank()) {
                return "Please provide a key to press.";
            }
            keyboardMovement.pressKey(key);
            waitForWindowRefresh();
            return "Successfully pressed key: " + key;
        } catch (Exception e) {
            return "Error pressing key: " + e.getMessage();
        }
    }

    @Tool("Presses a key combination (e.g., 'Ctrl+C', 'Alt+Tab', 'Ctrl+Shift+T'). Provide keys separated by '+'. Common modifiers: CTRL, ALT, SHIFT, CMD (Mac Command key).")
    public String pressKeyCombination(String combination) {
        try {
            if (combination == null || combination.isBlank()) {
                return "Please provide a key combination (e.g., 'Ctrl+C').";
            }
            String[] keys = combination.split("\\+");
            keyboardMovement.pressKeyCombination(keys);
            waitForWindowRefresh();
            return "Successfully pressed key combination: " + combination;
        } catch (Exception e) {
            return "Error pressing key combination: " + e.getMessage();
        }
    }
}
