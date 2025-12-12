package com.org.pp.finAgent.controller;

import com.org.pp.finAgent.automation.MouseMovement;
import com.org.pp.finAgent.service.OcrService;
import com.org.pp.finAgent.util.ScreenCapture;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class OCRController {
    private static final Logger LOGGER = Logger.getLogger(OCRController.class.getName());
    private static final int DEFAULT_COLOR_TOLERANCE = 40;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private MouseMovement mouseMovement;

    /**
     * Finds all occurrences of text matching a specific color and performs a
     * Ctrl+Click on each one.
     * It uses fuzzy matching for the text (e.g., "xyz" will match "xyz_asd").
     *
     * @param textToFind The text of the elements to find and multi-select.
     * @param hexColor   The hex color string (e.g., "#99c3ff") of the text to find.
     * @return The number of items that were successfully clicked.
     */
    public int findAndCtrlClickAllByTextAndColor(String textToFind, String hexColor) {
        LOGGER.info(
                "Attempting to find and Ctrl+Click all occurrences of: '" + textToFind + "' with color " + hexColor);
        int clickCount = 0;
        try {
            String screenCapturePath = ScreenCapture.captureToFile();
            BufferedImage image = ImageIO.read(new File(screenCapturePath));

            // This now uses the fuzzy matching logic from OcrService
            List<OcrService.OcrResult> results = ocrService.getWordsFromImage(image, textToFind);

            if (results.isEmpty()) {
                LOGGER.warning("Could not find any occurrences of the text '" + textToFind + "' on the screen.");
                return 0;
            }

            Color targetColor = Color.decode(hexColor);

            List<OcrService.OcrResult> filteredResults = results.stream()
                    .filter(result -> isWordColor(image, result, targetColor, DEFAULT_COLOR_TOLERANCE))
                    .collect(Collectors.toList());

            if (filteredResults.isEmpty()) {
                LOGGER.warning("Found text '" + textToFind + "', but none matched the color " + hexColor);
                return 0;
            }

            LOGGER.info("Found " + filteredResults.size()
                    + " occurrences with matching color. Proceeding to Ctrl+Click each one.");

            for (OcrService.OcrResult result : filteredResults) {
                if (clickOcrResult(result, "MOVE_AND_CTRL_CLICK")) {
                    clickCount++;
                    Thread.sleep(250); // A short pause between clicks is good for reliability
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "An error occurred reading the screen capture image.", e);
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "The click operation was interrupted.", e);
            Thread.currentThread().interrupt(); // Preserve the interrupted status
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE,
                    "An error occurred during the find-and-Ctrl+Click operation for text: '" + textToFind + "'", e);
        }
        return clickCount;
    }

    /**
     * Finds all text on screen matching a specific color and performs a Ctrl+Click
     * on each unique link. Words on the same line are grouped together to avoid
     * clicking the same link multiple times.
     *
     * @param hexColor The hex color string (e.g., "#5A9CFD") of the text to find.
     * @return The number of unique links that were successfully clicked.
     */
    public int openAllGoogleSearchLinks(String hexColor) {
        LOGGER.info("Attempting to find and Ctrl+Click all text with color: " + hexColor);
        int clickCount = 0;
        try {
            String screenCapturePath = ScreenCapture.captureToFile();
            BufferedImage image = ImageIO.read(new File(screenCapturePath));

            // Get ALL words from the screen without text filtering
            List<OcrService.OcrResult> allWords = ocrService.getAllWordsFromImage(image);

            if (allWords.isEmpty()) {
                LOGGER.warning("Could not detect any text on the screen.");
                return 0;
            }

            LOGGER.info("Detected " + allWords.size() + " words on screen. Filtering by color...");

            Color targetColor = Color.decode(hexColor);

            List<OcrService.OcrResult> filteredResults = allWords.stream()
                    .filter(result -> isWordColor(image, result, targetColor, DEFAULT_COLOR_TOLERANCE))
                    .collect(Collectors.toList());

            if (filteredResults.isEmpty()) {
                LOGGER.warning("Found text on screen, but none matched the color " + hexColor);
                return 0;
            }

            LOGGER.info("Found " + filteredResults.size() + " words with matching color. Grouping by line...");

            // Group words by their approximate Y position (same line = same link)
            // Use the first word of each line group as the click target
            List<OcrService.OcrResult> uniqueLinks = groupWordsByLine(filteredResults);

            LOGGER.info("Grouped into " + uniqueLinks.size() + " unique link(s). Proceeding to Ctrl+Click each one.");

            for (OcrService.OcrResult result : uniqueLinks) {
                if (clickOcrResult(result, "MOVE_AND_CTRL_CLICK")) {
                    clickCount++;
                    Thread.sleep(250); // A short pause between clicks for reliability
                }
            }

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "An error occurred reading the screen capture image.", e);
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "The click operation was interrupted.", e);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "An error occurred during the find-and-Ctrl+Click-by-color operation.", e);
        }
        return clickCount;
    }

    /**
     * Groups OCR results by their vertical position (Y coordinate).
     * Words within a vertical tolerance are considered to be on the same line
     * (and thus part of the same link). Returns only the first word from each line
     * group.
     *
     * @param results The list of OCR results to group.
     * @return A list containing only the first word from each line group.
     */
    private List<OcrService.OcrResult> groupWordsByLine(List<OcrService.OcrResult> results) {
        if (results.isEmpty()) {
            return results;
        }

        // Sort by Y position first, then by X position
        List<OcrService.OcrResult> sorted = results.stream()
                .sorted((a, b) -> {
                    int yCompare = Integer.compare(a.boundingBox().y, b.boundingBox().y);
                    if (yCompare != 0)
                        return yCompare;
                    return Integer.compare(a.boundingBox().x, b.boundingBox().x);
                })
                .collect(Collectors.toList());

        List<OcrService.OcrResult> uniqueLinks = new java.util.ArrayList<>();
        int lastY = -1000; // Initialize to a value that won't match
        int lineHeightTolerance = 15; // Pixels - words within this vertical distance are on the same line

        for (OcrService.OcrResult result : sorted) {
            int currentY = result.boundingBox().y;

            // If this word is not on the same line as the previous one, it's a new link
            if (Math.abs(currentY - lastY) > lineHeightTolerance) {
                uniqueLinks.add(result);
                lastY = currentY;
            }
        }

        return uniqueLinks;
    }

    /**
     * Checks if the text within a bounding box in an image likely matches a target
     * color.
     * This is a heuristic that samples a few points. It might need tuning.
     *
     * @param image       The image to check.
     * @param ocrResult   The OCR result containing the bounding box.
     * @param targetColor The color to look for.
     * @param tolerance   The allowed color distance. A higher value means more
     *                    lenient matching.
     * @return true if a sampled pixel matches the target color.
     */
    private boolean isWordColor(BufferedImage image, OcrService.OcrResult ocrResult, Color targetColor, int tolerance) {
        Rectangle box = ocrResult.boundingBox();
        // Define sample points within the bounding box to check for color
        int[][] samplePoints = {
                { box.x + box.width / 2, box.y + box.height / 2 }, // Center
                { box.x + box.width / 4, box.y + box.height / 2 }, // Mid-left
                { box.x + 3 * box.width / 4, box.y + box.height / 2 } // Mid-right
        };

        for (int[] point : samplePoints) {
            int px = point[0];
            int py = point[1];

            // Ensure point is within image bounds
            if (px >= 0 && px < image.getWidth() && py >= 0 && py < image.getHeight()) {
                Color pixelColor = new Color(image.getRGB(px, py));
                if (isColorSimilar(pixelColor, targetColor, tolerance)) {
                    // If any sample point matches, we consider it a success.
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Compares two colors based on their squared Euclidean distance in the RGB
     * space.
     *
     * @param c1        The first color.
     * @param c2        The second color.
     * @param tolerance The maximum allowed distance.
     * @return true if the distance is within the tolerance.
     */
    private boolean isColorSimilar(Color c1, Color c2, int tolerance) {
        long r_dist = c1.getRed() - c2.getRed();
        long g_dist = c1.getGreen() - c2.getGreen();
        long b_dist = c1.getBlue() - c2.getBlue();

        // Using squared distance to avoid expensive square root calculation.
        long distanceSq = r_dist * r_dist + g_dist * g_dist + b_dist * b_dist;
        long toleranceSq = (long) tolerance * tolerance;

        return distanceSq < toleranceSq;
    }

    /**
     * Private helper to perform a click action on a given OCR result.
     * 
     * @param target The OcrResult to click.
     * @param action The click action to perform (e.g., "MOVE_AND_CLICK",
     *               "MOVE_AND_CTRL_CLICK").
     * @return true, as the click command was executed.
     */
    private boolean clickOcrResult(OcrService.OcrResult target, String action) {
        Rectangle boundingBox = target.boundingBox();
        LOGGER.info(String.format("Executing %s on '%s' at [x=%d, y=%d, w=%d, h=%d] with confidence %.2f%%",
                action, target.text(), boundingBox.x, boundingBox.y, boundingBox.width, boundingBox.height,
                target.confidence()));

        int clickX = boundingBox.x + (boundingBox.width / 2);
        int clickY = boundingBox.y + (boundingBox.height / 2);

        String commandJson = String.format(
                "{\"action\":\"%s\", \"x\":%d, \"y\":%d, \"button\":\"LEFT\"}",
                action, clickX, clickY);

        mouseMovement.executeCommand(commandJson);
        return true;
    }
}