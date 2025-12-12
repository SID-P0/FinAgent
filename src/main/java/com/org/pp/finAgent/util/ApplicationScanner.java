package com.org.pp.finAgent.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Utility class for scanning and discovering installed applications on Windows
 * and macOS
 */
public class ApplicationScanner {

    private final boolean isWindows;
    private final boolean isMac;

    public ApplicationScanner() {
        String os = System.getProperty("os.name").toLowerCase();
        this.isWindows = os.contains("win");
        this.isMac = os.contains("mac");
    }

    /**
     * Scans the system for installed applications and returns a map of app names to
     * their paths
     */
    public Map<String, String> scanInstalledApplications() {
        Map<String, String> applicationCache = new HashMap<>();

        if (isWindows) {
            scanWindowsStartMenu(applicationCache);
        } else if (isMac) {
            scanMacApplications(applicationCache);
        }

        return applicationCache;
    }

    private void scanWindowsStartMenu(Map<String, String> applicationCache) {
        // Common Start Menu locations
        List<String> startMenuPaths = new ArrayList<>();

        String userStartMenu = System.getenv("APPDATA") + "\\Microsoft\\Windows\\Start Menu\\Programs";
        String commonStartMenu = System.getenv("ProgramData") + "\\Microsoft\\Windows\\Start Menu\\Programs";

        startMenuPaths.add(userStartMenu);
        startMenuPaths.add(commonStartMenu);

        for (String menuPath : startMenuPaths) {
            Path path = Paths.get(menuPath);
            if (Files.exists(path)) {
                try (Stream<Path> walk = Files.walk(path, 3)) {
                    walk.filter(p -> p.toString().toLowerCase().endsWith(".lnk"))
                            .forEach(p -> {
                                String fileName = p.getFileName().toString();
                                String appName = fileName.substring(0, fileName.length() - 4); // Remove .lnk
                                applicationCache.put(appName, p.toString());
                            });
                } catch (IOException e) {
                    // Ignore errors for inaccessible directories
                }
            }
        }

        // Also add some common applications by executable path
        addCommonWindowsApps(applicationCache);
    }

    private void addCommonWindowsApps(Map<String, String> applicationCache) {
        Map<String, String> commonApps = new HashMap<>();

        // Browser paths
        commonApps.put("Google Chrome", "C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe");
        commonApps.put("Microsoft Edge", "C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe");
        commonApps.put("Firefox", "C:\\Program Files\\Mozilla Firefox\\firefox.exe");

        // Office apps
        commonApps.put("Notepad", "notepad.exe");
        commonApps.put("Calculator", "calc.exe");
        commonApps.put("File Explorer", "explorer.exe");

        // Add if exists
        for (Map.Entry<String, String> entry : commonApps.entrySet()) {
            String path = entry.getValue();
            if (!path.contains("\\") || new File(path).exists()) {
                applicationCache.putIfAbsent(entry.getKey(), path);
            }
        }
    }

    private void scanMacApplications(Map<String, String> applicationCache) {
        Path applicationsPath = Paths.get("/Applications");
        if (Files.exists(applicationsPath)) {
            try (Stream<Path> walk = Files.walk(applicationsPath, 2)) {
                walk.filter(p -> p.toString().endsWith(".app"))
                        .forEach(p -> {
                            String fileName = p.getFileName().toString();
                            String appName = fileName.substring(0, fileName.length() - 4); // Remove .app
                            applicationCache.put(appName, p.toString());
                        });
            } catch (IOException e) {
                // Ignore errors
            }
        }

        // Also check user Applications
        String userHome = System.getProperty("user.home");
        Path userApps = Paths.get(userHome, "Applications");
        if (Files.exists(userApps)) {
            try (Stream<Path> walk = Files.walk(userApps, 2)) {
                walk.filter(p -> p.toString().endsWith(".app"))
                        .forEach(p -> {
                            String fileName = p.getFileName().toString();
                            String appName = fileName.substring(0, fileName.length() - 4);
                            applicationCache.put(appName, p.toString());
                        });
            } catch (IOException e) {
                // Ignore errors
            }
        }
    }
}
