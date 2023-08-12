package com.nfssoundtrack.creditstest;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import org.apache.commons.text.WordUtils;
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
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
public class NameModelHelper {

    public static NameFinderME nameFinder;

    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) throws IOException {
        InputStream is = new FileInputStream("src/main/resources/tessdata/en-ner-person.bin");
        // load the model from file
        TokenNameFinderModel model = new TokenNameFinderModel(is);
        is.close();
        // feed the model to name finder class
        nameFinder = new NameFinderME(model);
    }

    public static String replaceSomeCharacters(String imageText) {
        StringBuilder parsedText = new StringBuilder();
        String[] sentence = imageText.split("\n");
//        String[] noWhiteSpaceSentence = new String[sentence.length];
        for (int i = 0; i < sentence.length; i++) {
            String[] splitByComma = sentence[i].split(",");
            for (int j = 0; j < splitByComma.length; j++) {
                String camelCased =
                        WordUtils.capitalizeFully(splitByComma[j].trim());
                parsedText.append(camelCased).append("\n");
            }
            //allWordsOneByOne.addAll(List.of(splitByWhitespace));
            //noWhiteSpaceSentence[i] = sentence[i].split(" ");
        }
//        String[] noWhiteSpaceSentence = allWordsOneByOne.toArray(new String[0]);
//        Span[] nameSpans = nameFinder.find(noWhiteSpaceSentence);
//        String[] spans2 = Span.spansToStrings(nameSpans, noWhiteSpaceSentence);
        return parsedText.toString();
    }

    public static String analyzeSentence(String imageText) {
        List<String> allWordsOneByOne = new ArrayList<>();
        String[] sentence = imageText.split("\n");
//        String[] noWhiteSpaceSentence = new String[sentence.length];
        for (int i = 0; i < sentence.length; i++) {
            String[] splitByWhitespace = sentence[i].split(" ");
            allWordsOneByOne.addAll(List.of(splitByWhitespace));
            //noWhiteSpaceSentence[i] = sentence[i].split(" ");
        }
        String[] noWhiteSpaceSentence = allWordsOneByOne.toArray(new String[0]);
        Span[] nameSpans = nameFinder.find(noWhiteSpaceSentence);
        String[] spans2 = Span.spansToStrings(nameSpans, noWhiteSpaceSentence);
        return imageText;
    }

    public static void invertImage(Path imagePath, String originalFileName, boolean grayscale) {
        BufferedImage inputFile = null;
        try {
            inputFile = ImageIO.read(imagePath.toFile());
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (int x = 0; x < inputFile.getWidth(); x++) {
            for (int y = 0; y < inputFile.getHeight(); y++) {
                int rgba = inputFile.getRGB(x, y);
                Color col = new Color(rgba, true);
//                int red = col.getRed();
//                int green = col.getGreen();
//                int blue = col.getBlue();
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
        try {
            String dedicatedPath = imagePath.toFile().getPath();
            dedicatedPath = dedicatedPath.replace(originalFileName, "invert_" + originalFileName);
            File outputFile = new File(dedicatedPath);
            ImageIO.write(inputFile, "png", outputFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
