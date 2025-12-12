package com.org.pp.finAgent.util;

import java.util.concurrent.TimeUnit;

/**
 * Utility class for bringing application windows to focus on different
 * platforms
 */
public class WindowFocusHelper {

    private final boolean isWindows;
    private final boolean isMac;

    public WindowFocusHelper() {
        String os = System.getProperty("os.name").toLowerCase();
        this.isWindows = os.contains("win");
        this.isMac = os.contains("mac");
    }

    /**
     * Brings Chrome window to focus on Windows or macOS
     * 
     * @return true if successful, false otherwise
     */
    public boolean bringChromeToFocus() {
        try {
            if (isWindows) {
                // Use PowerShell to bring Chrome to foreground
                String script = "(New-Object -ComObject WScript.Shell).AppActivate('Chrome')";
                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", script);
                Process process = pb.start();
                process.waitFor(2, TimeUnit.SECONDS);
                return true;
            } else if (isMac) {
                // Use AppleScript to activate Chrome
                String[] cmd = { "osascript", "-e", "tell application \"Google Chrome\" to activate" };
                ProcessBuilder pb = new ProcessBuilder(cmd);
                Process process = pb.start();
                process.waitFor(2, TimeUnit.SECONDS);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Brings any application window to focus by name
     * 
     * @param appName The name of the application to focus
     * @return true if successful, false otherwise
     */
    public boolean bringApplicationToFocus(String appName) {
        try {
            if (isWindows) {
                String script = "(New-Object -ComObject WScript.Shell).AppActivate('" + appName + "')";
                ProcessBuilder pb = new ProcessBuilder("powershell.exe", "-Command", script);
                Process process = pb.start();
                process.waitFor(2, TimeUnit.SECONDS);
                return true;
            } else if (isMac) {
                String[] cmd = { "osascript", "-e", "tell application \"" + appName + "\" to activate" };
                ProcessBuilder pb = new ProcessBuilder(cmd);
                Process process = pb.start();
                process.waitFor(2, TimeUnit.SECONDS);
                return true;
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}
