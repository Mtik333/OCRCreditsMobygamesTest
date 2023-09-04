package com.nfssoundtrack.creditstest;

import net.sourceforge.tess4j.Tesseract;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import org.apache.commons.lang3.SystemUtils;
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
import java.nio.file.Path;

@Component
public class NameModelHelper {

    private static final Logger logger = LoggerFactory.getLogger(NameModelHelper.class);

    public static NameFinderME nameFinder;
    public final static boolean isLinux = SystemUtils.IS_OS_LINUX;
    public static boolean isJarMode;

    @SuppressWarnings("unused")
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
            jarUrl = null;
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
//                if (capitalizeDevNames){
//                    s = WordUtils.capitalizeFully(s.trim());
//                }
//                logger.debug("camelcased sentence: " + s);
                parsedText.append(s).append("\n");
            }
        }
        sentence = null;
        return parsedText.toString();
    }

    public static void resizeAndInvertImage(Path imagePath, String originalFileName, boolean grayscale,
                                            boolean isBlack, int imageResolutionMultiplier)
            throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("starting invertImage with " + imagePath + ", grayscale? " + grayscale);
        }
        BufferedImage inputFile = ImageIO.read(imagePath.toFile());
        if (imageResolutionMultiplier != 1) {
            int newWidth = (inputFile.getWidth() * imageResolutionMultiplier);
            int newHeight = (inputFile.getHeight() * imageResolutionMultiplier);
            Image resizedImage = inputFile.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
            String resizedPath = imagePath.toFile().getPath();
            String actualFileName = imagePath.getFileName().toString();
            resizedPath = resizedPath.replace(originalFileName, "resized_" + originalFileName);
            File resizedPathAsFile = new File(resizedPath);
            if (!resizedPathAsFile.exists()) {
                resizedPathAsFile.createNewFile();
            }
            boolean write = ImageIO.write(convertToBufferedImage(resizedImage), "png", resizedPathAsFile);
            inputFile = ImageIO.read(resizedPathAsFile);
        }
        int actualWidth = inputFile.getWidth();
        int actualHeight = inputFile.getHeight();
        if (isBlack) {
            for (int x = 0; x < actualWidth; x++) {
                for (int y = 0; y < actualHeight; y++) {
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
            dedicatedPath = null;
        }
        inputFile = null;
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

    public static BufferedImage convertToBufferedImage(Image img) {
        if (img instanceof BufferedImage) {
            return (BufferedImage) img;
        }
        // Create a buffered image with transparency
        BufferedImage bi = new BufferedImage(
                img.getWidth(null), img.getHeight(null),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = bi.createGraphics();
        graphics2D.drawImage(img, 0, 0, null);
        graphics2D.dispose();
        return bi;
    }
}
