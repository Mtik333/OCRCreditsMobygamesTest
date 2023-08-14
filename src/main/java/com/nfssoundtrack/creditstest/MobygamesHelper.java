package com.nfssoundtrack.creditstest;

import com.lowagie.text.html.HtmlEncoder;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MobygamesHelper {

    private static final Logger logger = LoggerFactory.getLogger(MobygamesHelper.class);

    public static Map<String, String> reworkResultDevUnder(Map<Path, String> filesWithText,
                                                           boolean nicknameDetect) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("starting reworkResultDevUnder with " + filesWithText + ", nicknameDetect " + nicknameDetect);
        }
        InputStream is;
        if (NameModelHelper.isJarMode) {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            is = classloader.getResourceAsStream("tessdata/en-ner-person.bin");
        } else {
            is = new FileInputStream("src/main/resources/tessdata/en-ner-person.bin");
        }
        // load the model from file
        if (logger.isDebugEnabled()) {
            logger.debug("inputstream " + is);
        }
        TokenNameFinderModel model = new TokenNameFinderModel(is);
        is.close();
        NameFinderME nameFinderME = new NameFinderME(model);
        if (logger.isDebugEnabled()) {
            logger.debug("nameFinderME " + nameFinderME);
        }
        boolean groupStarted = false;
        boolean roleStarted = false;
        boolean devToAppend = false;
        Map<String, String> reworkedMap = new HashMap<>();
        for (Map.Entry<Path, String> entry : filesWithText.entrySet()) {
            StringBuilder producedString = new StringBuilder();
            String[] splitTextByNewline = entry.getValue().split("\n");
            String lineString;
            String nextLine;
            for (int i = 0; i < splitTextByNewline.length; i++) {
                lineString = splitTextByNewline[i].trim();
                nextLine = null;
                if ((i + 1) != splitTextByNewline.length) {
                    nextLine = splitTextByNewline[i + 1].trim();
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("lineString " + lineString);
                    logger.debug("nextLine " + lineString);
                    logger.debug("groupStarted " + groupStarted);
                    logger.debug("roleStarted " + roleStarted);
                }
                if (!groupStarted) {
                    //check if next line is empty - if yes then it is a group
                    //otherwise push group to stringbuilder
                    if (!nextLine.isBlank()) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("custom group created by tool");
                        }
                        producedString.append("<Insert Group Title Here>").append("\n\n");
                        groupStarted = true;
                        producedString.append(lineString).append("\n");
                        roleStarted = isNotHumanName(lineString, nameFinderME);
                        continue;
                    } else {
                        if (logger.isDebugEnabled()) {
                            logger.debug("very first group: " + lineString);
                        }
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
                    lineString = getRidOfNicknames(lineString);
                    roleStarted = isNotHumanName(lineString, nameFinderME);
                    devToAppend = !roleStarted;
                    producedString.append(lineString).append("\n");
                    if (roleStarted && nextLine == null) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("appending new line at the end of image");
                        }
                        producedString.append("\n");
                    }
                }
            }
            String fileIdName = entry.getKey().getFileName().toString();
            fileIdName = fileIdName.replace("invert_", "");
            fileIdName = fileIdName.substring(0, fileIdName.indexOf("."));
            String resultOfTool = producedString.toString();
            if (logger.isDebugEnabled()) {
                logger.debug("adding entry: " + fileIdName + ", value: " + resultOfTool);
            }
            reworkedMap.put(fileIdName, HtmlEncoder.encode(resultOfTool));
        }
        return reworkedMap;
    }

    public static Map<String, String> reworkResultDevNext(Map<Path, String> filesWithText,
                                                          boolean twoWordNames,
                                                          boolean nicknameDetect) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("starting reworkResultDevNext with " + filesWithText
                    + ", nicknameDetect " + nicknameDetect + ", twoWordNames " + twoWordNames);
        }
        InputStream is;
        if (NameModelHelper.isJarMode) {
            ClassLoader classloader = Thread.currentThread().getContextClassLoader();
            is = classloader.getResourceAsStream("tessdata/en-ner-person.bin");
        } else {
            is = new FileInputStream("src/main/resources/tessdata/en-ner-person.bin");
        }
        TokenNameFinderModel model = new TokenNameFinderModel(is);
        is.close();
        NameFinderME nameFinderME = new NameFinderME(model);
        if (logger.isDebugEnabled()) {
            logger.debug("nameFinderME " + nameFinderME);
        }
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
                if (logger.isDebugEnabled()) {
                    logger.debug("lineString " + lineString);
                    logger.debug("nextLine " + lineString);
                    logger.debug("groupStarted " + groupStarted);
                    logger.debug("roleStarted " + roleStarted);
                }
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
                        if (nextLine != null) {
                            AbstractMap.SimpleEntry<String, String> analyzedLine =
                                    roleToDevName(nextLine, nameFinderME, twoWordNames);

                            if (analyzedLine.getValue() != null) {
                                logger.warn("not sure why i got here with " + analyzedLine);
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
                    }
                } else {
                    AbstractMap.SimpleEntry<String, String> analyzedLine =
                            roleToDevName(lineString, nameFinderME, twoWordNames);
                    if (analyzedLine.getValue() != null) {
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
                }
            }
            String fileIdName = entry.getKey().getFileName().toString();
            fileIdName = fileIdName.replace("invert_", "");
            fileIdName = fileIdName.substring(0, fileIdName.indexOf("."));
            String resultOfTool = producedString.toString();
            if (logger.isDebugEnabled()) {
                logger.debug("adding entry: " + fileIdName + ", value: " + resultOfTool);
            }
            reworkedMap.put(fileIdName, HtmlEncoder.encode(resultOfTool));
            resultOfTool= null;
        }
        return reworkedMap;
    }

    private static boolean isNotHumanName(String lineString, NameFinderME nameFinderME) {
        if (logger.isDebugEnabled()) {
            logger.debug("starting isNotHumanName with " + lineString);
        }
        boolean roleStarted = false;
        String[] lineDividedByWhiteSpace = lineString.split(" ");
        Span[] nameSpans = nameFinderME.find(lineDividedByWhiteSpace);
        if (logger.isDebugEnabled()) {
            logger.debug("nameSpans? " + Arrays.toString(nameSpans));
        }
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
                firstSpan=null;
            }
        }
        if (logger.isDebugEnabled()) {
            logger.debug("roleStarted " + roleStarted);
        }
        nameSpans = null;
        lineDividedByWhiteSpace = null;
        return roleStarted;
    }

    private static AbstractMap.SimpleEntry<String, String> roleToDevName(String lineString,
                                                                         NameFinderME nameFinderME,
                                                                         boolean twoWordNames) {
        //assuming line is: 3D Artist John Smith - we want to get 2 last words and check if this is a name
        if (logger.isDebugEnabled()) {
            logger.debug("starting roleToDevName with " + lineString + ", twoWordNames " + twoWordNames);
        }
        String[] lineDividedByWhiteSpace = lineString.split(" ");
        int divisionIndex = lineDividedByWhiteSpace.length - 2;
        if (logger.isDebugEnabled()) {
            logger.debug("divisionIndex " + divisionIndex);
        }
        StringBuilder roleBulder = new StringBuilder();
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 0; i < lineDividedByWhiteSpace.length; i++) {
            if (i < divisionIndex) {
                roleBulder.append(lineDividedByWhiteSpace[i]).append(" ");
            } else {
                nameBuilder.append(lineDividedByWhiteSpace[i]).append(" ");
            }
        }
        String roleBuilderLine = roleBulder.toString().trim();
        String nameBuilderLine = nameBuilder.toString().trim();
        lineDividedByWhiteSpace=null;
        roleBulder=null;
        nameBuilder=null;
        if (logger.isDebugEnabled()) {
            logger.debug("roleBuilderLine " + roleBuilderLine + ", nameBuilderLine " + nameBuilderLine);
        }
        if (twoWordNames) {
            return new AbstractMap.SimpleEntry<>(roleBuilderLine, nameBuilderLine);
        } else {
            String[] devLine = nameBuilder.toString().split(" ");
            Span[] nameSpans = nameFinderME.find(devLine);
            devLine = null;
            if (logger.isDebugEnabled()) {
                logger.debug("nameSpans? " + Arrays.toString(nameSpans));
            }
            if (nameSpans.length == 0) {
                //??????????
                nameSpans = null;
                return new AbstractMap.SimpleEntry<>(lineString, null);
            } else {
                nameSpans = null;
                return new AbstractMap.SimpleEntry<>(roleBuilderLine, nameBuilderLine);
            }
        }
    }

    private static String getRidOfNicknames(String lineWithNick) {
        if (logger.isDebugEnabled()) {
            logger.debug("starting getRidOfNicknames with " + lineWithNick);
        }
        boolean nicknameExists = (StringUtils.countMatches(lineWithNick, "'") +
                StringUtils.countMatches(lineWithNick, "\"") +
                StringUtils.countMatches(lineWithNick, "’")) == 2;
        if (logger.isDebugEnabled()) {
            logger.debug("nicknameExists " + nicknameExists);
        }
        if (nicknameExists) {
            lineWithNick = lineWithNick.replace("’", "'").replace("\"", "'");
            int firstIndex = lineWithNick.indexOf("'");
            int lastIndex = lineWithNick.lastIndexOf("'");
            String nickName = lineWithNick.substring(firstIndex + 1, lastIndex);
            String finalName = lineWithNick.substring(0, firstIndex - 1) + lineWithNick.substring(lastIndex + 1) + " ('" + nickName + "')";
            if (logger.isDebugEnabled()) {
                logger.debug("finalName " + finalName);
            }
            nickName = null;
            lineWithNick = null;
            return finalName.trim();
        } else {
            return lineWithNick;
        }
    }
}
