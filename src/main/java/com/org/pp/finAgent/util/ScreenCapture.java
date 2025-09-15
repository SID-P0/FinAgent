package com.org.pp.finAgent.util;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class ScreenCapture {

    /**
     * Captures the entire screen, saves a timestamped debug copy to the 'debug_screenshots' folder,
     * and returns the image as a raw byte array (PNG format).
     *
     * @return A byte array containing the raw PNG image data of the screen capture.
     * @throws AWTException     if the platform configuration does not allow low-level input control.
     * @throws IOException      if an error occurs during writing the image.
     * @throws RuntimeException if screen access is denied (e.g., on macOS without permissions).
     */
    public static byte[] captureAsBytes() throws AWTException, IOException {
        // 1. Get the captured image from the helper method
        BufferedImage capture = performScreenCapture();

        // 2. Save a debug copy with a unique timestamp
        String timestamp = new java.text.SimpleDateFormat("yyyyMMdd-HHmmss-SSS").format(new java.util.Date());
        File debugFile = new File("debug_screenshots/capture-" + timestamp + ".png");
        debugFile.getParentFile().mkdirs(); // Ensure the directory exists
        ImageIO.write(capture, "png", debugFile);
        System.out.println("Debug screenshot saved to: " + debugFile.getAbsolutePath());

        // 3. Convert the same image to a byte array and return it
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(capture, "png", baos);
            return baos.toByteArray();
        }
    }

    /**
     * Private helper to perform the actual screen capture and handle errors.
     * This avoids code duplication in the public methods.
     */
    private static BufferedImage performScreenCapture() throws AWTException, IOException {
        try {
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            return new Robot().createScreenCapture(screenRect);
        } catch (NullPointerException e) {
            // This handles the common macOS permission issue gracefully by providing a clear error
            throw new RuntimeException(
                    "Failed to get screen size. This is likely a macOS permissions issue.\n" +
                            "Please grant 'Screen Recording' permission to your IDE or Terminal in:\n" +
                            "System Settings > Privacy & Security > Screen Recording", e);
        }
    }
}