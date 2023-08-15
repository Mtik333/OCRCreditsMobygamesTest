package com.nfssoundtrack.creditstest;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

public class ResultsHolder {

    private final Logger logger = LoggerFactory.getLogger(ResultsHolder.class);

    private List<Path> allFiles;
    private Map<Path, String> resultsPerFile;

    public List<Path> getAllFiles() {
        return allFiles;
    }

    public void setAllFiles(List<Path> allFiles) {
        this.allFiles = allFiles;
    }

    public Map<Path, String> getResultsPerFile() {
        return resultsPerFile;
    }

    public void setResultsPerFile(Map<Path, String> resultsPerFile) {
        this.resultsPerFile = resultsPerFile;
    }

    public void performReading(Tesseract tesseractInstance, Integer segMode,
                               Integer ocrEngineMode, Rectangle rectangle,
                               boolean replaceComma) throws IOException, TesseractException, URISyntaxException {
        for (Path path : allFiles) {
            byte[] imageBytes = Files.readAllBytes(path);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (logger.isDebugEnabled()) {
                logger.debug("image fetched");
            }
            String textResult = getTextFromImageNoRest(tesseractInstance, segMode, ocrEngineMode,
                    rectangle, img);
            img = null;
            imageBytes = null;
            if (logger.isDebugEnabled()) {
                logger.debug("textResult " + textResult);
            }
            String result2 = textResult.replaceAll("\n\n\n+", "\n\n");
            if (replaceComma) {
                result2 = NameModelHelper.replaceSomeCharacters(result2);
            }
            if (logger.isDebugEnabled()) {
                logger.debug("putting entry in map:  " + path + ", val: " + result2);
            }
            resultsPerFile.put(path, result2);
        }
    }

    public String getTextFromImageNoRest(Tesseract tesseractInstance, Integer segMode,
                                         Integer ocrEngineMode, Rectangle rectangle, BufferedImage img
    ) throws URISyntaxException, TesseractException {
        if (logger.isDebugEnabled()) {
            logger.debug("starting cleanSessionImagesDirectory with " + tesseractInstance
                    + ", segMode" + segMode + ", ocrEngineMode " + ocrEngineMode
                    + ", rectangle " + rectangle + ", img " + img);
        }
        if (tesseractInstance == null) {
            tesseractInstance = new Tesseract();
            NameModelHelper.setTesseractDatapath(tesseractInstance);
            tesseractInstance.setLanguage("eng");
            if (segMode != null) {
                tesseractInstance.setPageSegMode(segMode);
            }
            if (ocrEngineMode != null) {
                tesseractInstance.setOcrEngineMode(ocrEngineMode);
            }
        }
        String result = tesseractInstance.doOCR(img, rectangle);
        if (logger.isDebugEnabled()) {
            logger.debug("OCR done successfully");
        }
        return result;
    }

    public AbstractMap.Entry<String, String> giveFeedback() {
        if (resultsPerFile != null) {
            int inputSize = getResultsPerFile().size();
            int finalSize = getAllFiles().size();
            if (inputSize != finalSize) {
                return new AbstractMap.SimpleEntry<>("ocr", "OCRed images: "
                        + resultsPerFile.size() + "/" + allFiles.size());
            } else return new AbstractMap.SimpleEntry<>("ocr", "Performing post-OCR grouping...");
        } else {
            return new AbstractMap.SimpleEntry<>("ocr", "Process has just begun...");
        }
    }
}
