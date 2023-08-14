package com.nfssoundtrack.creditstest;

import net.sourceforge.tess4j.Tesseract;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import org.apache.commons.lang3.SystemUtils;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

@Component
public class NameModelHelper {

    private static final Logger logger = LoggerFactory.getLogger(NameModelHelper.class);

    public static NameFinderME nameFinder;
    public final static boolean isLinux = SystemUtils.IS_OS_LINUX;
    public static boolean isJarMode;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) {
        logger.info("Starting app " + event.toString());
        try {
            InputStream is;
            String jarUrl = NameModelHelper.class.getProtectionDomain()
                    .getCodeSource().getLocation().getPath();
            logger.info("jarUrl " + jarUrl);
            if (jarUrl != null) {
                isJarMode = jarUrl.startsWith("file:/");
            } else {
                isJarMode = false;
            }
            if (isJarMode) {
                ClassLoader classloader = Thread.currentThread().getContextClassLoader();
                is = classloader.getResourceAsStream("tessdata/en-ner-person.bin");
            } else {
                is = new FileInputStream("src/main/resources/tessdata/en-ner-person.bin");
            }
            // load the model from file
            TokenNameFinderModel model = new TokenNameFinderModel(is);
            is.close();
            // feed the model to name finder class
            nameFinder = new NameFinderME(model);
            jarUrl=null;
        } catch (Throwable throwable) {
            logger.error("something wrong happened on start: " + throwable.getMessage());
            throwable.printStackTrace();
        }
    }

    public static String replaceSomeCharacters(String imageText) {
        if (logger.isDebugEnabled()) {
            logger.debug("starting replaceSomeCharacters with: " + imageText);
        }
        StringBuilder parsedText = new StringBuilder();
        String[] sentence = imageText.split("\n");
        for (String value : sentence) {
            if (logger.isDebugEnabled()) {
                logger.debug("value of sentence: " + value);
            }
            String[] splitByComma = value.split(",");
            for (String s : splitByComma) {
                String camelCased =
                        WordUtils.capitalizeFully(s.trim());
                logger.debug("camelcased sentence: " + camelCased);
                parsedText.append(camelCased).append("\n");
                camelCased=null;
            }
        }
        sentence = null;
        return parsedText.toString();
    }

    public static void invertImage(Path imagePath, String originalFileName, boolean grayscale) throws IOException {

        if (logger.isDebugEnabled()) {
            logger.debug("starting invertImage with " + imagePath + ", grayscale? " + grayscale);
        }
        BufferedImage inputFile = ImageIO.read(imagePath.toFile());
        for (int x = 0; x < inputFile.getWidth(); x++) {
            for (int y = 0; y < inputFile.getHeight(); y++) {
                int rgba = inputFile.getRGB(x, y);
                Color col = new Color(rgba, true);
                //invert image
                int red = 255 - col.getRed();
                int green = 255 - col.getGreen();
                int blue = 255 - col.getBlue();
                if (grayscale) {
                    red = (int) (red * 0.299);
                    green = (int) (green * 0.587);
                    blue = (int) (blue * 0.114);
                    col = new Color(red + green + blue, red + green + blue, red + green + blue);
                } else {
                    col = new Color(red, green, blue);
                }
                inputFile.setRGB(x, y, col.getRGB());
            }
        }
        String dedicatedPath = imagePath.toFile().getPath();
        dedicatedPath = dedicatedPath.replace(originalFileName, "invert_" + originalFileName);
        File outputFile = new File(dedicatedPath);
        ImageIO.write(inputFile, "png", outputFile);
        inputFile=null;
        dedicatedPath = null;
    }

    public static void setTesseractDatapath(Tesseract tesseractInstance) throws URISyntaxException {
        if (logger.isDebugEnabled()) {
            logger.debug("starting setTesseractDatapath with " + tesseractInstance
                    + ", isjarmode? " + isJarMode + ", isLinux? " + isLinux);
        }
        if (!isJarMode) {
            tesseractInstance.setDatapath("src/main/resources/tessdata");
        } else {
            URI myUri = GreetingController.class.getProtectionDomain()
                    .getCodeSource().getLocation().toURI();
            String almostFinalPath;
            if (isLinux) {
                almostFinalPath = myUri.toString().replace("jar:file:", "");
            } else {
                almostFinalPath = myUri.toString().replace("jar:file:/", "");
            }
            if (logger.isDebugEnabled()) {
                logger.debug("almostFinalPath: " + almostFinalPath);
            }
            String stillNotFinalPath = almostFinalPath.substring(0, almostFinalPath.indexOf("!") - 1);
            if (logger.isDebugEnabled()) {
                logger.debug("stillNotFinalPath: " + stillNotFinalPath);
            }
            int lastIndexOfSepratar = stillNotFinalPath.lastIndexOf("/");
            String finalPath = stillNotFinalPath.substring(0, lastIndexOfSepratar);
            if (logger.isDebugEnabled()) {
                logger.debug("finalPath: " + finalPath);
            }
            tesseractInstance.setDatapath(finalPath);
        }
    }

}
