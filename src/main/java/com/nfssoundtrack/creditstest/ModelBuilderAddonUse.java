package com.nfssoundtrack.creditstest;

import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.util.Span;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ModelBuilderAddonUse {

    //fill this method in with however you are going to get your data into a list of sentences..for me I am hitting a MySQL database
    private static List<String> getSentencesFromSomewhere() throws Exception {
        List<String> sentences = new ArrayList<>();
        File sentencesFile = new File("src/main/resources/tessdata/mobygames.train");
        sentences = Files.readAllLines(sentencesFile.toPath());
        return sentences;
    }

    public static void main(String[] args) throws Exception {
        /**
         * establish a file to put sentences in
         */
        File sentences = new File("src/main/resources/tessdata/sentences.txt");
        /**
         * establish a file to put your NER hits in (the ones you want to keep based
         * on prob)
         */
        File knownEntities = new File("src/main/resources/tessdata/knownentities.txt");
        /**
         * establish a BLACKLIST file to put your bad NER hits in (also can be based
         * on prob)
         */
        File blacklistedentities = new File("src/main/resources/tessdata/blentities.txt");
        /**
         * establish a file to write your annotated sentences to
         */
        File annotatedSentences = new File("src/main/resources/tessdata/annotatedSentences.txt");
        /**
         * establish a file to write your model to
         */
        File theModel = new File("src/main/resources/tessdata/theModel");
//------------create a bunch of file writers to write your results and sentences to a file
        FileWriter sentenceWriter = new FileWriter(sentences, true);
        FileWriter blacklistWriter = new FileWriter(blacklistedentities, true);
        FileWriter knownEntityWriter = new FileWriter(knownEntities, true);
//set some thresholds to decide where to write hits, you don't have to use these at all...
        double keeperThresh = .95;
        double blacklistThresh = .7;
        /**
         * Load your model as normal
         */
        TokenNameFinderModel personModel = new TokenNameFinderModel(new File("src/main/resources/tessdata/en-ner-person.bin"));
        NameFinderME personFinder = new NameFinderME(personModel);
        /**
         * do your normal NER on the sentences you have
         */
        for (String s : getSentencesFromSomewhere()) {
            sentenceWriter.write(s.trim() + "\n");
            sentenceWriter.flush();

            String[] tokens = s.split(" ");//better to use a tokenizer really
            Span[] find = personFinder.find(tokens);
            double[] probs = personFinder.probs();
            String[] names = Span.spansToStrings(find, tokens);
            for (int i = 0; i < names.length; i++) {
                //YOU PROBABLY HAVE BETTER HEURISTICS THAN THIS TO MAKE SURE YOU GET GOOD HITS OUT OF THE DEFAULT MODEL
                if (probs[i] > keeperThresh) {
                    knownEntityWriter.write(names[i].trim() + "\n");
                }
                if (probs[i] < blacklistThresh) {
                    blacklistWriter.write(names[i].trim() + "\n");
                }
            }
            personFinder.clearAdaptiveData();
            blacklistWriter.flush();
            knownEntityWriter.flush();
        }
        //flush and close all the writers
        knownEntityWriter.flush();
        knownEntityWriter.close();
        sentenceWriter.flush();
        sentenceWriter.close();
        blacklistWriter.flush();
        blacklistWriter.close();
        OutputStream modelOut = null;
        try {
            modelOut = new BufferedOutputStream(new FileOutputStream("src/main/resources/tessdata/en-ner-person2.bin"));
            personModel.serialize(modelOut);
        } finally {
            if (modelOut != null)
                modelOut.close();
        }
        /**
         * THIS IS WHERE THE ADDON IS GOING TO USE THE FILES (AS IS) TO CREATE A NEW MODEL. YOU SHOULD NOT HAVE TO RUN THE FIRST PART AGAIN AFTER THIS RUNS, JUST NOW PLAY WITH THE
         * KNOWN ENTITIES AND BLACKLIST FILES AND RUN THE METHOD BELOW AGAIN UNTIL YOU GET SOME DECENT RESULTS (A DECENT MODEL OUT OF IT).
         */

    }
}
