package com.nfssoundtrack.creditstest;

import com.lowagie.text.html.HtmlEncoder;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class MobygamesHelper {

    public static Map<String, String> reworkResult(Map<Path, String> filesWithText) throws IOException {
        InputStream is = new FileInputStream("src/main/resources/tessdata/en-ner-person.bin");
        // load the model from file
        TokenNameFinderModel model = new TokenNameFinderModel(is);
        is.close();
        NameFinderME nameFinderME = new NameFinderME(model);
        // feed the model to name finder class
        boolean groupStarted = false;
        boolean roleStarted=false;
        boolean devToAppend=false;
        Map<String, String> reworkedMap = new HashMap<>();
        for (Map.Entry<Path, String> entry : filesWithText.entrySet()){
            StringBuilder producedString = new StringBuilder();
            String[] splitTextByNewline = entry.getValue().split("\n");
            for (int i=0; i<splitTextByNewline.length; i++){
                String lineString = splitTextByNewline[i];
                String nextLine = null;
                if ((i+1)!=splitTextByNewline.length){
                    nextLine = splitTextByNewline[i+1];
                }
//                String nextLineAfterNext = splitTextByNewline[i+2];
                if (!groupStarted){
                    //check if next line is empty - if yes then it is a group
                    //otherwise push group to stringbuilder
                    if (!nextLine.isBlank()){
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
                if (lineString.isBlank()){
                    if (!roleStarted){
                        producedString.append("\n");
                    } else {
                        devToAppend = !isNotHumanName(nextLine, nameFinderME);
                        if (!devToAppend){
                            producedString.append("\n");
                        }
                        roleStarted = false;
                    }
                } else {
//                    String[] lineDividedByWhiteSpace = lineString.split(" ");
//                    Span[] nameSpans = nameFinderME.find(lineDividedByWhiteSpace);
                    roleStarted = isNotHumanName(lineString, nameFinderME);
                    devToAppend = !roleStarted;
                    producedString.append(lineString).append("\n");
                    if (roleStarted && nextLine==null){
                        producedString.append("\n");
                    }
                    System.out.println("???");
                }
            }
            String fileIdName = entry.getKey().getFileName().toString();
            fileIdName = fileIdName.replace("invert_","");
            fileIdName = fileIdName.substring(0,fileIdName.indexOf("."));
            reworkedMap.put(fileIdName, HtmlEncoder.encode(producedString.toString()));
        }
        return reworkedMap;
    }

    private static boolean isNotHumanName(String lineString, NameFinderME nameFinderME){
        boolean roleStarted=false;
        String[] lineDividedByWhiteSpace = lineString.split(" ");
        Span[] nameSpans = nameFinderME.find(lineDividedByWhiteSpace);
        if (nameSpans.length==0){
            //some shitt example is 'pascal torfs'
//            if (lineDividedByWhiteSpace.length>1){
//                return isNotHumanName(lineString.replace(lineDividedByWhiteSpace[1], "John"), nameFinderME);
//            }
            roleStarted=true;
        } else {
            Span firstSpan = nameSpans[0];
            if (firstSpan.getStart()!=0){
                roleStarted=true;
            }
        }
        return roleStarted;
    }
}
