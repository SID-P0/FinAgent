package com.org.pp.finAgent.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Configuration class for Tesseract OCR that automatically detects
 * the appropriate tessdata path based on the operating system.
 */
@Configuration
public class TesseractConfig {

    private static final Logger log = LoggerFactory.getLogger(TesseractConfig.class);

    // Windows default path
    private static final String WINDOWS_DEFAULT_PATH = "C:/Program Files/Tesseract-OCR/tessdata";

    // macOS paths (Homebrew)
    private static final String MACOS_ARM_PATH = "/opt/homebrew/share/tessdata"; // Apple Silicon
    private static final String MACOS_INTEL_PATH = "/usr/local/share/tessdata"; // Intel Mac

    private final String tessDataPath;

    public TesseractConfig(@Value("${tesseract.datapath.override:}") String overridePath) {
        if (overridePath != null && !overridePath.isBlank()) {
            this.tessDataPath = overridePath;
            log.info("Using overridden Tesseract datapath: {}", tessDataPath);
        } else {
            this.tessDataPath = detectDefaultPath();
            log.info("Auto-detected Tesseract datapath: {}", tessDataPath);
        }

        // Validate that the path exists
        File tessDataDir = new File(tessDataPath);
        if (!tessDataDir.exists() || !tessDataDir.isDirectory()) {
            log.warn("Tesseract data directory not found at: {}. OCR functionality may not work.", tessDataPath);
        }
    }

    /**
     * Detects the default Tesseract tessdata path based on the operating system.
     */
    private String detectDefaultPath() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return WINDOWS_DEFAULT_PATH;
        } else if (os.contains("mac")) {
            // Check Apple Silicon path first, then Intel
            if (new File(MACOS_ARM_PATH).exists()) {
                return MACOS_ARM_PATH;
            } else if (new File(MACOS_INTEL_PATH).exists()) {
                return MACOS_INTEL_PATH;
            }
            // Default to ARM path (more common now)
            return MACOS_ARM_PATH;
        } else {
            // Linux or other Unix-like systems
            return "/usr/share/tesseract-ocr/4.00/tessdata";
        }
    }

    /**
     * Returns the configured Tesseract tessdata path.
     */
    public String getTessDataPath() {
        return tessDataPath;
    }
}
