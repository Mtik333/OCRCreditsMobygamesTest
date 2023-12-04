package com.nfssoundtrack.creditstest;


import com.itextpdf.text.html.HtmlEncoder;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;

public class MobygamesHelper {

    private static final Logger logger = LoggerFactory.getLogger(MobygamesHelper.class);

    public static Map<String, String> reworkResultDevUnder(Map<Path, String> filesWithText,
                                                           boolean nicknameDetect,
                                                           boolean capitalizeDevNames,
                                                           boolean capitalizeRoles,
                                                           String uppercaseKeywords) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("starting reworkResultDevUnder with " + filesWithText + ", nicknameDetect " + nicknameDetect);
        }
        List<String> keywords = new ArrayList<>();
        if (uppercaseKeywords != null) {
            String[] preKeywords = uppercaseKeywords.split(",");
            for (String keyword : preKeywords) {
                keywords.add(keyword.trim());
            }
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
                    if (nextLine != null && !nextLine.isBlank()) {
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
                    } else if (nextLine != null) {
                        devToAppend = !isNotHumanName(nextLine, nameFinderME);
                        if (!devToAppend) {
                            producedString.append("\n");
                        }
                        roleStarted = false;
                    }
                } else {
                    lineString = getRidOfNicknames(lineString);
                    if (capitalizeDevNames && roleStarted) {
                        lineString = WordUtils.capitalizeFully(lineString);
                    } else if (capitalizeRoles) {
                        lineString = WordUtils.capitalizeFully(lineString);
                        lineString = uppercaseWithKeywordsConstraint(lineString, keywords);
                    }
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
                                                          boolean nicknameDetect,
                                                          boolean capitalizeDevNames,
                                                          boolean capitalizeRoles,
                                                          String uppercaseKeywords,
                                                          boolean rolesOnLeft,
                                                          String lineSeparator) throws IOException {
        if (logger.isDebugEnabled()) {
            logger.debug("starting reworkResultDevNext with " + filesWithText
                    + ", nicknameDetect " + nicknameDetect + ", twoWordNames " + twoWordNames);
        }
        List<String> keywords = new ArrayList<>();
        if (uppercaseKeywords != null) {
            String[] preKeywords = uppercaseKeywords.split(",");
            for (String keyword : preKeywords) {
                keywords.add(keyword.trim());
            }
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
            List<String> producerStringList = new ArrayList<>();
//            StringBuilder producedString = new StringBuilder();
            String[] splitTextByNewline = entry.getValue().split("\n");
            boolean emptyLineExists = isEmptyLineInOCR(splitTextByNewline);
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
//                        producedString.append("<Insert Group Title Here>\n").append("\n");
                        producerStringList.add("<Insert Group Title Here>\n");
                        groupStarted = true;
                        AbstractMap.SimpleEntry<String, String> analyzedLine =
                                roleToDevName(lineString, nameFinderME, twoWordNames,
                                        roleStarted, capitalizeDevNames, capitalizeRoles,
                                        getPreviousFromList(producerStringList, 1),
                                        getPreviousFromList(producerStringList, 2),
                                        keywords, rolesOnLeft, emptyLineExists, lineSeparator);
                        if (analyzedLine.getValue() != null) {
//                            producedString.append(analyzedLine.getKey()).append("\n").append(analyzedLine.getValue());
                            producerStringList.add(analyzedLine.getKey() + ("\n") + analyzedLine.getValue());
                        } else {
//                            producedString.append(lineString).append("\n");
                            producerStringList.add(lineString);
                        }
                        roleStarted = isNotHumanName(lineString, nameFinderME);
                        continue;
                    } else {
                        groupStarted = true;
//                        producedString.append(lineString).append("\n");
                        producerStringList.add(lineString);
                        continue;
                    }
                }
                if (lineString.isBlank()) {
                    if (!roleStarted) {
//                        producedString.append("\n");
                        producerStringList.add("");
                    } else {
                        roleStarted = false;
                        if (nextLine != null) {
                            AbstractMap.SimpleEntry<String, String> analyzedLine =
                                    roleToDevName(nextLine, nameFinderME, twoWordNames, roleStarted, capitalizeDevNames, capitalizeRoles,
                                            getPreviousFromList(producerStringList, 1),
                                            getPreviousFromList(producerStringList, 2),
                                            keywords, rolesOnLeft, emptyLineExists, lineSeparator);
                            if (analyzedLine.getValue() != null) {
                                producerStringList.add("");
                            } else {
                                nextLine = getRidOfNicknames(nextLine);
                                devToAppend = !isNotHumanName(nextLine, nameFinderME);
                                if (!devToAppend) {
//                                    producedString.append("\n\n");
                                    producerStringList.add("");
                                }
                                roleStarted = false;
                                groupStarted = false;
                            }
                        }
                    }
                } else {
                    lineString = getRidOfNicknames(lineString);
                    AbstractMap.SimpleEntry<String, String> analyzedLine =
                            roleToDevName(lineString, nameFinderME, twoWordNames,
                                    roleStarted, capitalizeDevNames, capitalizeRoles,
                                    getPreviousFromList(producerStringList, 1),
                                    getPreviousFromList(producerStringList, 2),
                                    keywords, rolesOnLeft, emptyLineExists, lineSeparator);
                    if (analyzedLine.getValue() != null) {
                        if (analyzedLine.getKey().isEmpty()) {
//                            producedString.append("\n").append(lineString);
                            producerStringList.add(lineString);
                        } else {
                            if (roleStarted) {
//                                producedString.append("\n\n");
                                producerStringList.add("");
                            }
//                            producedString.append(analyzedLine.getKey()).append("\n").append(analyzedLine.getValue());
                            producerStringList.add(analyzedLine.getKey() + "\n" + analyzedLine.getValue());
                            roleStarted = true;
                        }
                    } else {
                        if (roleStarted) {
//                            producedString.append("\n");
                            producerStringList.add("");
                        }
//                        producedString.append(lineString).append("\n");
                        producerStringList.add(lineString);
                    }
                }
            }
            String fileIdName = entry.getKey().getFileName().toString();
            fileIdName = fileIdName.replace("invert_", "");
            fileIdName = fileIdName.substring(0, fileIdName.indexOf("."));
//            String resultOfTool = producedString.toString();
            String resultOfTool2 = String.join("\n", producerStringList);
            if (logger.isDebugEnabled()) {
                logger.debug("adding entry: " + fileIdName + ", value: " + resultOfTool2);
            }
            reworkedMap.put(fileIdName, HtmlEncoder.encode(resultOfTool2));
            resultOfTool2 = null;
        }
        return reworkedMap;
    }


    public static Map<String, String> reworkResultDevNextFulLCredits(Map<Path, String> filesWithText,
                                                                     boolean twoWordNames,
                                                                     boolean nicknameDetect,
                                                                     boolean capitalizeDevNames,
                                                                     boolean capitalizeRoles,
                                                                     String uppercaseKeywords,
                                                                     boolean rolesOnLeft,
                                                                     String lineSeparator) {
        if (logger.isDebugEnabled()) {
            logger.debug("starting reworkResultDevNextFulLCredits with " + filesWithText
                    + ", nicknameDetect " + nicknameDetect + ", twoWordNames " + twoWordNames);
        }
        List<String> keywords = new ArrayList<>();
        if (uppercaseKeywords != null) {
            String[] preKeywords = uppercaseKeywords.split(",");
            for (String keyword : preKeywords) {
                keywords.add(keyword.trim());
            }
        }
        Map<String, Map<String, List<String>>> groupsToRolesWithDevs = new HashMap<>();
        for (Map.Entry<Path, String> entry : filesWithText.entrySet()) {
            String[] splitTextByNewline = entry.getValue().split("\n");
            for (int i = 0; i < splitTextByNewline.length; i++) {
                String lineString = splitTextByNewline[i];
                if (logger.isDebugEnabled()) {
                    logger.debug("lineString " + lineString);
                }
                roleToDevNameToGroup(lineString, capitalizeDevNames, capitalizeRoles, keywords, null, lineSeparator, groupsToRolesWithDevs);
            }
        }
        //we are going to return just one file
        Map<String, String> reworkedMap = new HashMap<>();
        Map.Entry<Path, String> firstEntry = filesWithText.entrySet().stream().findFirst().get();
        String fileIdName = firstEntry.getKey().getFileName().toString();
        fileIdName = fileIdName.replace("invert_", "");
        fileIdName = fileIdName.substring(0, fileIdName.indexOf("."));
        String resultOfTool2 = fullCreditsFormat(groupsToRolesWithDevs);
        reworkedMap.put(fileIdName, HtmlEncoder.encode(resultOfTool2));
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
                firstSpan = null;
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
                                                                         boolean twoWordNames,
                                                                         boolean roleStarted,
                                                                         boolean capitalizeDevNames,
                                                                         boolean capitalizeRoles,
                                                                         String previousAppend,
                                                                         String appendBeforePreviousAppend,
                                                                         List<String> keywords,
                                                                         boolean rolesOnLeft,
                                                                         boolean emptyLineExists,
                                                                         String lineSeparator) {
        //assuming line is: 3D Artist John Smith - we want to get 2 last words and check if this is a name
        if (logger.isDebugEnabled()) {
            logger.debug("starting roleToDevName with " + lineString + ", twoWordNames " + twoWordNames);
        }
        if (roleStarted && !previousAppend.isBlank() && !appendBeforePreviousAppend.isBlank()
                && emptyLineExists) {
            return new AbstractMap.SimpleEntry<>("", lineString);
        }
        StringBuilder roleBulder = new StringBuilder();
        StringBuilder nameBuilder = new StringBuilder();
        String roleBuilderLine;
        String nameBuilderLine;
        if (!lineSeparator.isEmpty()) {
            String[] lineDividedByChar = lineString.split(lineSeparator);
            if (lineDividedByChar.length < 2) {
                return new AbstractMap.SimpleEntry<>(" ", " ");
            }
            if (rolesOnLeft) {
                roleBuilderLine = lineDividedByChar[0].trim();
                nameBuilderLine = lineDividedByChar[1].trim();
            } else {
                roleBuilderLine = lineDividedByChar[1].trim();
                nameBuilderLine = lineDividedByChar[0].trim();
            }
        } else {
            String[] lineDividedByWhiteSpace = lineString.split(" ");
            if (rolesOnLeft) {
                int divisionIndex = lineDividedByWhiteSpace.length - 2;
                if (logger.isDebugEnabled()) {
                    logger.debug("divisionIndex " + divisionIndex);
                }
                for (int i = 0; i < lineDividedByWhiteSpace.length; i++) {
                    if (i < divisionIndex) {
                        roleBulder.append(lineDividedByWhiteSpace[i]).append(" ");
                    } else {
                        nameBuilder.append(lineDividedByWhiteSpace[i]).append(" ");
                    }
                }
            } else {
                int divisionIndex = 1;
                if (logger.isDebugEnabled()) {
                    logger.debug("divisionIndex " + divisionIndex);
                }
                for (int i = 0; i < lineDividedByWhiteSpace.length; i++) {
                    if (i > divisionIndex) {
                        roleBulder.append(lineDividedByWhiteSpace[i]).append(" ");
                    } else {
                        nameBuilder.append(lineDividedByWhiteSpace[i]).append(" ");
                    }
                }
            }
            roleBuilderLine = roleBulder.toString().trim();
            nameBuilderLine = nameBuilder.toString().trim();
        }
        if (capitalizeDevNames) {
            nameBuilderLine = WordUtils.capitalizeFully(nameBuilderLine);
        }
        if (capitalizeRoles) {
            roleBuilderLine = WordUtils.capitalizeFully(roleBuilderLine);
            roleBuilderLine = uppercaseWithKeywordsConstraint(roleBuilderLine, keywords);
        }
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
                roleBulder = null;
                nameBuilder = null;
                return new AbstractMap.SimpleEntry<>(lineString, null);
            } else {
                nameSpans = null;
                roleBulder = null;
                nameBuilder = null;
                return new AbstractMap.SimpleEntry<>(roleBuilderLine, nameBuilderLine);
            }
        }
    }

    private static void roleToDevNameToGroup(String lineString,
                                             boolean capitalizeDevNames,
                                             boolean capitalizeRoles,
                                             List<String> keywords,
                                             String formatOfLine,
                                             String lineSeparator,
                                             Map<String, Map<String, List<String>>> groupsToRolesWithDevs
    ) {
        if (logger.isDebugEnabled()) {
            logger.debug("starting roleToDevNameToGroup with lineString: " + lineString
                    + ", capitalizeDevNames " + capitalizeDevNames + ", capitalizeRoles " + capitalizeRoles
                    + ", formatOfLine " + formatOfLine + ", lineSeparator: " + lineSeparator);
        }
        if (!lineSeparator.isEmpty()) {
            String[] lineDividedByChar = lineString.split(" " + lineSeparator);
            //TODO make it flexible
            if (lineDividedByChar.length >= 3) {
                String dev = lineDividedByChar[0].trim();
                String role = lineDividedByChar[2].trim();
                String group = lineDividedByChar[1].trim();
                if (capitalizeDevNames) {
                    dev = WordUtils.capitalizeFully(dev);
                }
                if (capitalizeRoles) {
                    role = WordUtils.capitalizeFully(role);
                    role = uppercaseWithKeywordsConstraint(role, keywords);
                }
                logger.debug("dev after capitalizing " + dev);
                boolean groupExists = groupsToRolesWithDevs.containsKey(group);
                if (groupExists) {
                    logger.debug("groupExists " + group);
                    Map<String, List<String>> roleToDevs = groupsToRolesWithDevs.get(group);
                    boolean roleExists = roleToDevs.containsKey(role);
                    if (roleExists) {
                        logger.debug("roleExists " + role);
                        List<String> devs = roleToDevs.get(role);
                        if (!devs.contains(dev)) {
                            devs.add(dev);
                        }
                        roleToDevs.put(role, devs);
                    } else {
                        logger.debug("creating role: " + role);
                        List<String> devs = new ArrayList<>();
                        devs.add(dev);
                        roleToDevs.put(role, devs);
                    }
                } else {
                    logger.debug("creating group: " + group);
                    List<String> devs = new ArrayList<>();
                    devs.add(dev);
                    HashMap<String, List<String>> roleToDev = new HashMap<>();
                    roleToDev.put(role, devs);
                    groupsToRolesWithDevs.put(group, roleToDev);
                }
            }
        }
    }

    private static String getRidOfNicknames(String lineWithNick) {
        if (logger.isDebugEnabled()) {
            logger.debug("starting getRidOfNicknames with " + lineWithNick);
        }
        lineWithNick = lineWithNick.replace("“", "'")
                .replace("”", "'")
                .replace("\"", "'")
                .replace("’", "'");
        boolean nicknameExists = StringUtils.countMatches(lineWithNick, "'") == 2;
        if (logger.isDebugEnabled()) {
            logger.debug("nicknameExists " + nicknameExists);
        }
        if (nicknameExists) {
            try {
                lineWithNick = lineWithNick.replace("’", "'").replace("\"", "'");
                int firstIndex = lineWithNick.indexOf("'");
                int lastIndex = lineWithNick.lastIndexOf("'");
                if (firstIndex == 0 && lastIndex == lineWithNick.length() - 1) {
                    //this basically means the whole phrase is in quotation marks so there's no nickname
                    return lineWithNick;
                }
                String nickName = lineWithNick.substring(firstIndex + 1, lastIndex);
                String finalName = lineWithNick.substring(0, firstIndex - 1) + lineWithNick.substring(lastIndex + 1) + " ('" + nickName + "')";
                if (logger.isDebugEnabled()) {
                    logger.debug("finalName " + finalName);
                }
                nickName = null;
                lineWithNick = null;
                return finalName.trim();
            } catch (Throwable thr) {
                logger.warn("couldnt fix nickname but it might be because of some OCR issue: " + lineWithNick);
                return lineWithNick;
            }
        } else {
            return lineWithNick;
        }
    }

    private static String getPreviousFromList(List<String> appended, int back) {
        int backIndex = appended.size() - back;
        if (backIndex > 0) {
            return appended.get(backIndex);
        } else return "";
    }

    private static String uppercaseWithKeywordsConstraint(String lineString, List<String> keywords) {
        String[] elems = lineString.split(" ");
        for (int i = 0; i < elems.length; i++) {
            String potentialUppercase = elems[i].toUpperCase();
            if (keywords.contains(potentialUppercase)) {
                elems[i] = potentialUppercase;
            }
        }
        return String.join(" ", elems);
    }

    private static boolean isEmptyLineInOCR(String[] splitTextByNewline) {
        for (String line : splitTextByNewline) {
            if (line.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static String fullCreditsFormat(Map<String, Map<String, List<String>>> groupsToRolesWithDevs) {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Map<String, List<String>>> groupEntry : groupsToRolesWithDevs.entrySet()) {
            String group = groupEntry.getKey();
            builder.append(group).append("\n\n");
            Map<String, List<String>> roleToDevs = groupEntry.getValue();
            for (Map.Entry<String, List<String>> roleEntry : roleToDevs.entrySet()) {
                String role = roleEntry.getKey();
                List<String> devs = roleEntry.getValue();
                builder.append(role).append("\n");
                for (String dev : devs) {
                    builder.append(dev).append("\n");
                }
                builder.append("\n");
            }
        }
        return builder.toString();
    }
}
