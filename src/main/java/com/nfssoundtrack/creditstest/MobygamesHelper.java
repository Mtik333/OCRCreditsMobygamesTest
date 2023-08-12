package com.nfssoundtrack.creditstest;

import com.lowagie.text.html.HtmlEncoder;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import org.apache.commons.lang3.StringUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;

public class MobygamesHelper {

    public static Map<String, String> reworkResultDevUnder(Map<Path, String> filesWithText, boolean nicknameDetect)
            throws IOException {
        InputStream is = new FileInputStream("src/main/resources/tessdata/en-ner-person.bin");
        // load the model from file
        TokenNameFinderModel model = new TokenNameFinderModel(is);
        is.close();
        NameFinderME nameFinderME = new NameFinderME(model);
        // feed the model to name finder class
        boolean groupStarted = false;
        boolean roleStarted = false;
        boolean devToAppend = false;
        Map<String, String> reworkedMap = new HashMap<>();
        for (Map.Entry<Path, String> entry : filesWithText.entrySet()) {
            StringBuilder producedString = new StringBuilder();
            String[] splitTextByNewline = entry.getValue().split("\n");
            for (int i = 0; i < splitTextByNewline.length; i++) {
                String lineString = splitTextByNewline[i];
                String nextLine = null;
                if ((i + 1) != splitTextByNewline.length) {
                    nextLine = splitTextByNewline[i + 1];
                }
//                String nextLineAfterNext = splitTextByNewline[i+2];
                if (!groupStarted) {
                    //check if next line is empty - if yes then it is a group
                    //otherwise push group to stringbuilder
                    if (!nextLine.isBlank()) {
                        producedString.append("<Insert Group Title Here>").append("\n\n");
                        groupStarted = true;
                        producedString.append(lineString).append("\n");
                        roleStarted = isNotHumanName(lineString, nameFinderME);
                        continue;
                    } else {
                        groupStarted = true;
                        producedString.append(lineString).append("\n");
                        continue;
                    }
                }
                if (lineString.isBlank()) {
                    if (!roleStarted) {
                        producedString.append("\n");
                    } else {
                        devToAppend = !isNotHumanName(nextLine, nameFinderME);
                        if (!devToAppend) {
                            producedString.append("\n");
                        }
                        roleStarted = false;
                    }
                } else {
//                    String[] lineDividedByWhiteSpace = lineString.split(" ");
//                    Span[] nameSpans = nameFinderME.find(lineDividedByWhiteSpace);
                    lineString = getRidOfNicknames(lineString);
                    roleStarted = isNotHumanName(lineString, nameFinderME);
                    devToAppend = !roleStarted;
                    producedString.append(lineString).append("\n");
                    if (roleStarted && nextLine == null) {
                        producedString.append("\n");
                    }
                    System.out.println("???");
                }
            }
            String fileIdName = entry.getKey().getFileName().toString();
            fileIdName = fileIdName.replace("invert_", "");
            fileIdName = fileIdName.substring(0, fileIdName.indexOf("."));
            reworkedMap.put(fileIdName, HtmlEncoder.encode(producedString.toString()));
        }
        return reworkedMap;
    }

    public static Map<String, String> reworkResultDevNext(Map<Path, String> filesWithText,
                                                          boolean twoWordNames,
                                                          boolean nicknameDetect) throws IOException {
        InputStream is = new FileInputStream("src/main/resources/tessdata/en-ner-person.bin");
        // load the model from file
        TokenNameFinderModel model = new TokenNameFinderModel(is);
        is.close();
        NameFinderME nameFinderME = new NameFinderME(model);
        // feed the model to name finder class
        boolean groupStarted = false;
        boolean roleStarted = false;
        boolean devToAppend = false;
        Map<String, String> reworkedMap = new HashMap<>();
        for (Map.Entry<Path, String> entry : filesWithText.entrySet()) {
            StringBuilder producedString = new StringBuilder();
            String[] splitTextByNewline = entry.getValue().split("\n");
            for (int i = 0; i < splitTextByNewline.length; i++) {
                String lineString = splitTextByNewline[i];
                String nextLine = null;
                if ((i + 1) != splitTextByNewline.length) {
                    nextLine = splitTextByNewline[i + 1];
                }
//                String nextLineAfterNext = splitTextByNewline[i+2];
                if (!groupStarted) {
                    //check if next line is empty - if yes then it is a group
                    //otherwise push group to stringbuilder
                    if (nextLine != null && !nextLine.isBlank()) {
                        producedString.append("<Insert Group Title Here>").append("\n\n");
                        groupStarted = true;
                        AbstractMap.SimpleEntry<String, String> analyzedLine =
                                roleToDevName(lineString, nameFinderME, twoWordNames);
                        if (analyzedLine.getValue() != null) {
                            producedString.append(analyzedLine.getKey()).append("\n").append(analyzedLine.getValue());
                        } else {
                            producedString.append(lineString).append("\n");
                        }
                        roleStarted = isNotHumanName(lineString, nameFinderME);
                        continue;
                    } else {
                        groupStarted = true;
                        producedString.append(lineString).append("\n");
                        continue;
                    }
                }
                if (lineString.isBlank()) {
                    if (!roleStarted) {
                        producedString.append("\n");
                    } else {
                        AbstractMap.SimpleEntry<String, String> analyzedLine =
                                roleToDevName(nextLine, nameFinderME, twoWordNames);
                        if (analyzedLine.getValue() != null) {
//                            producedString.append("\n");
//                                    .append(analyzedLine.getKey()).append("\n")
//                                    .append(analyzedLine.getValue());
                        } else {
                            nextLine = getRidOfNicknames(nextLine);
                            devToAppend = !isNotHumanName(nextLine, nameFinderME);
                            if (!devToAppend) {
                                producedString.append("\n\n");
                            }
                            roleStarted = false;
                            groupStarted = false;
                        }
                    }
                } else {
//                    String[] lineDividedByWhiteSpace = lineString.split(" ");
//                    Span[] nameSpans = nameFinderME.find(lineDividedByWhiteSpace);
                    AbstractMap.SimpleEntry<String, String> analyzedLine =
                            roleToDevName(lineString, nameFinderME, twoWordNames);
                    if (analyzedLine.getValue() != null) {
//                        if (roleStarted){
//                            producedString.append("\n\n");
//                        }
                        if (analyzedLine.getKey().isEmpty()) {
                            producedString.append("\n").append(lineString);
                        } else {
                            if (roleStarted) {
                                producedString.append("\n\n");
                            }
                            producedString.append(analyzedLine.getKey()).append("\n").append(analyzedLine.getValue());
                            roleStarted = true;
                        }
                    } else {
                        if (roleStarted) {
                            producedString.append("\n");
                        }
                        producedString.append(lineString).append("\n");
                    }
//                    roleStarted = isNotHumanName(lineString, nameFinderME);
//                    devToAppend = !roleStarted;
//                    producedString.append(lineString).append("\n");
//                    if (roleStarted && nextLine==null){
//                        producedString.append("\n");
//                    }
//                    System.out.println("???");
                }
            }
            String fileIdName = entry.getKey().getFileName().toString();
            fileIdName = fileIdName.replace("invert_", "");
            fileIdName = fileIdName.substring(0, fileIdName.indexOf("."));
            reworkedMap.put(fileIdName, HtmlEncoder.encode(producedString.toString()));
        }
        return reworkedMap;
    }

    private static boolean isNotHumanName(String lineString, NameFinderME nameFinderME) {
        boolean roleStarted = false;
        String[] lineDividedByWhiteSpace = lineString.split(" ");
        Span[] nameSpans = nameFinderME.find(lineDividedByWhiteSpace);
        if (nameSpans.length == 0) {
            //some shitt example is 'pascal torfs'
//            if (lineDividedByWhiteSpace.length>1){
//                return isNotHumanName(lineString.replace(lineDividedByWhiteSpace[1], "John"), nameFinderME);
//            }
            roleStarted = true;
        } else {
            Span firstSpan = nameSpans[0];
            if (firstSpan.getStart() != 0) {
                roleStarted = true;
            }
        }
        return roleStarted;
    }

    private static AbstractMap.SimpleEntry<String, String> roleToDevName(String lineString,
                                                                         NameFinderME nameFinderME,
                                                                         boolean twoWordNames) {
        //assuming line is: 3D Artist John Smith - we want to get 2 last words and check if this is a name
        String[] lineDividedByWhiteSpace = lineString.split(" ");
        int divisionIndex = lineDividedByWhiteSpace.length - 2;
        StringBuilder roleBulder = new StringBuilder();
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < lineDividedByWhiteSpace.length; i++) {
            if (i < divisionIndex) {
                roleBulder.append(lineDividedByWhiteSpace[i]).append(" ");
            } else {
                nameBuilder.append(lineDividedByWhiteSpace[i]).append(" ");
            }
        }
        if (twoWordNames) {
            return new AbstractMap.SimpleEntry<>(roleBulder.toString(), nameBuilder.toString());
        } else {
            String[] devLine = nameBuilder.toString().split(" ");
            Span[] nameSpans = nameFinderME.find(devLine);
            if (nameSpans.length == 0) {
                //??????????
                return new AbstractMap.SimpleEntry<>(lineString, null);
            } else {
                return new AbstractMap.SimpleEntry<>(roleBulder.toString(), nameBuilder.toString());
            }
        }
    }

    private static String getRidOfNicknames(String lineWithNick) {
        boolean nicknameExists = (StringUtils.countMatches(lineWithNick, "'") +
                StringUtils.countMatches(lineWithNick, "\"") +
                StringUtils.countMatches(lineWithNick, "’")) == 2;
        if (nicknameExists) {
            lineWithNick = lineWithNick.replace("’", "'").replace("\"", "'");
            int firstIndex = lineWithNick.indexOf("'");
            int lastIndex = lineWithNick.lastIndexOf("'");
            String nickName = lineWithNick.substring(firstIndex + 1, lastIndex);
            String finalName = lineWithNick.substring(0, firstIndex - 1) + lineWithNick.substring(lastIndex + 1) + " ('" + nickName + "')";
            return finalName;
        } else return lineWithNick;
    }
}
