package com.org.pp.finAgent.agent.tools;

import com.org.pp.finAgent.automation.KeyboardMovement;
import com.org.pp.finAgent.controller.OCRController;
import com.org.pp.finAgent.util.WindowFocusHelper;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

@Component
public class ChromeTools {
    private final KeyboardMovement keyboardMovement;
    private final WindowFocusHelper windowFocusHelper;
    private final boolean isMac;
    private final OCRController ocrController;

    public ChromeTools(KeyboardMovement keyboardMovement, OCRController ocrController) {
        this.ocrController = ocrController;
        this.keyboardMovement = keyboardMovement;
        this.windowFocusHelper = new WindowFocusHelper();
        String os = System.getProperty("os.name").toLowerCase();
        this.isMac = os.contains("mac");
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

    @Tool("Scans the screen using OCR to find the specified text and clicks on it. Use this to click on navigation elements, buttons, or any visible text on the screen.")
    public String findAndClickText(String textToFind) {
        try {
            boolean success = ocrController.findAndClickText(textToFind);
            waitForWindowRefresh();
            if (success) {
                return "Successfully found and clicked on '" + textToFind + "'.";
            } else {
                return "Could not find '" + textToFind + "' on the screen.";
            }
        } catch (Exception e) {
            return "Error during OCR operation: " + e.getMessage();
        }
    }

    @Tool("Finds and Ctrl+clicks all blue hyperlinks visible on the screen")
    public String clickAllBlueLinks() {
        // Blue link color - typical blue hyperlink color
        final String BLUE_LINK_COLOR = "#5A9CFD";
        try {
            int count = ocrController.openAllGoogleSearchLinks(BLUE_LINK_COLOR);
            waitForWindowRefresh();
            if (count > 0) {
                return "Successfully found and Ctrl+clicked " + count + " blue link(s).";
            } else {
                return "Could not find any blue links on the screen.";
            }
        } catch (Exception e) {
            return "Error during blue links detection: " + e.getMessage();
        }
    }

    @Tool("Searches for a query in Google Chrome. Chrome must be already open and will be brought to focus. The search is performed in the address bar.")
    public String searchInChrome(String query) {
        try {
            if (query == null || query.isBlank()) {
                return "Please provide a search query.";
            }

            // Bring Chrome to focus
            if (!windowFocusHelper.bringChromeToFocus()) {
                return "Failed to bring Chrome to focus. Make sure Chrome is open.";
            }

            // Wait for window to be in focus
            Thread.sleep(500);

            // Open address bar (Ctrl+L on Windows/Linux, Cmd+L on Mac)
            keyboardMovement.pressKeyCombination(new String[] { isMac ? "CMD" : "CTRL", "L" });

            // Wait for address bar to be ready
            Thread.sleep(200);

            // Type the search query
            keyboardMovement.typeText(query);

            // Press Enter
            Thread.sleep(100);
            keyboardMovement.pressKey("ENTER");

            waitForWindowRefresh();
            return "Successfully searched for '" + query + "' in Chrome.";
        } catch (Exception e) {
            return "Error searching in Chrome: " + e.getMessage();
        }
    }

    @Tool("Opens a new incognito window in Google Chrome. Chrome must be already open.")
    public String openNewTab() {
        try {
            if (!windowFocusHelper.bringChromeToFocus()) {
                return "Failed to bring Chrome to focus. Make sure Chrome is open.";
            }

            Thread.sleep(300);

            // Ctrl+Shift+N (Windows/Linux) or Cmd+Shift+N (Mac) for incognito
            keyboardMovement.pressKeyCombination(new String[] { isMac ? "CMD" : "CTRL", "SHIFT", "N" });

            waitForWindowRefresh();
            return "Successfully opened a new incognito window in Chrome.";
        } catch (Exception e) {
            return "Error opening incognito window: " + e.getMessage();
        }
    }

    @Tool("Closes the current tab in Google Chrome. Chrome must be already open.")
    public String closeCurrentTab() {
        try {
            if (!windowFocusHelper.bringChromeToFocus()) {
                return "Failed to bring Chrome to focus. Make sure Chrome is open.";
            }

            Thread.sleep(300);

            // Ctrl+W (Windows/Linux) or Cmd+W (Mac)
            keyboardMovement.pressKeyCombination(new String[] { isMac ? "CMD" : "CTRL", "W" });

            waitForWindowRefresh();
            return "Successfully closed the current tab in Chrome.";
        } catch (Exception e) {
            return "Error closing tab: " + e.getMessage();
        }
    }

    @Tool("Navigates to a specific URL in Google Chrome. Chrome must be already open. Provide the full URL including http:// or https://")
    public String navigateToUrl(String url) {
        try {
            if (url == null || url.isBlank()) {
                return "Please provide a URL.";
            }

            if (!windowFocusHelper.bringChromeToFocus()) {
                return "Failed to bring Chrome to focus. Make sure Chrome is open.";
            }

            Thread.sleep(500);

            // Open address bar
            keyboardMovement.pressKeyCombination(new String[] { isMac ? "CMD" : "CTRL", "L" });
            Thread.sleep(200);

            // Type the URL
            keyboardMovement.typeText(url);
            Thread.sleep(100);

            // Press Enter
            keyboardMovement.pressKey("ENTER");

            waitForWindowRefresh();
            return "Successfully navigated to '" + url + "' in Chrome.";
        } catch (Exception e) {
            return "Error navigating to URL: " + e.getMessage();
        }
    }
}
