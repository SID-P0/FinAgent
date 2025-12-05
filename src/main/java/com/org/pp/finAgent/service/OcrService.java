package com.org.pp.finAgent.service;

import com.org.pp.finAgent.configuration.TesseractConfig;
import com.org.pp.finAgent.exception.OcrProcessingException;
import net.sourceforge.tess4j.ITessAPI;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.Word;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OcrService {

    private final ITesseract tesseract;

    public OcrService(TesseractConfig tesseractConfig) {
        this.tesseract = new Tesseract();
        this.tesseract.setDatapath(tesseractConfig.getTessDataPath());
        this.tesseract.setLanguage("eng");
    }

    /**
     * Detects specific words in an image file and returns their location and
     * confidence.
     *
     * @param imagePath The path to the image file.
     * @param findWord  The word to search for within the image.
     * @return A list of OcrResult objects for each instance of the found word.
     */
    public List<OcrResult> getWordsFromImage(String imagePath, String findWord) {
        try {
            BufferedImage image = ImageIO.read(new File(imagePath));
            if (image == null) {
                throw new OcrProcessingException(
                        "Could not read image file, it may be null or corrupted: " + imagePath);
            }
            // Delegate to the method that works with a BufferedImage
            return getWordsFromImage(image, findWord);
        } catch (IOException e) {
            throw new OcrProcessingException("Failed to read image file at path: " + imagePath, e);
        }
    }

    /**
     * Detects all words in a BufferedImage and returns their location and
     * confidence.
     *
     * @param image The BufferedImage to process.
     * @return A list of OcrResult objects for every word found in the image.
     */
    public List<OcrResult> getAllWordsFromImage(BufferedImage image) {
        try {
            List<Word> words = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);

            return words.stream()
                    .filter(word -> word.getText() != null && !word.getText().trim().isEmpty())
                    .map(word -> new OcrResult(word.getText().trim(), word.getBoundingBox(), word.getConfidence()))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            throw new OcrProcessingException("An unexpected error occurred during OCR processing.", ex);
        }
    }

    /**
     * Detects specific words in a BufferedImage and returns their location and
     * confidence.
     * This version uses fuzzy matching to handle cases where OCR merges words
     * (e.g., "word1_word2").
     *
     * @param image    The BufferedImage to process.
     * @param findWord The word to search for within the image.
     * @return A list of OcrResult objects for each instance of the found word.
     */
    public List<OcrResult> getWordsFromImage(BufferedImage image, String findWord) {
        try {
            List<Word> words = tesseract.getWords(image, ITessAPI.TessPageIteratorLevel.RIL_WORD);

            return words.stream()
                    .filter(word -> word.getText() != null && !word.getText().trim().isEmpty())
                    // Updated filter to perform fuzzy matching
                    .filter(word -> isFuzzyMatch(word.getText(), findWord))
                    .map(word -> new OcrResult(word.getText(), word.getBoundingBox(), word.getConfidence()))
                    .collect(Collectors.toList());
        } catch (Exception ex) {
            throw new OcrProcessingException("An unexpected error occurred during OCR processing.", ex);
        }
    }

    /**
     * Checks if the search text is present within the OCR-detected text by
     * splitting
     * the OCR text by non-alphanumeric characters. For example, if OCR reads
     * "xyz_asd",
     * searching for "xyz" will return true.
     *
     * @param ocrText    The text detected by Tesseract.
     * @param searchText The text to find.
     * @return True if a match is found, false otherwise.
     */
    private boolean isFuzzyMatch(String ocrText, String searchText) {
        if (ocrText == null || searchText == null || searchText.isEmpty()) {
            return false;
        }
        // Split the OCR text by any sequence of one or more non-alphanumeric characters
        return Arrays.stream(ocrText.trim().split("[^a-zA-Z0-9]+"))
                .anyMatch(part -> searchText.equalsIgnoreCase(part));
    }

    public record OcrResult(String text, Rectangle boundingBox, float confidence) {

    }

}