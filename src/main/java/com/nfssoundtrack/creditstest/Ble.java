package com.nfssoundtrack.creditstest;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import opennlp.tools.namefind.*;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collections;

public class Ble {

    public static void main(String[] args) throws TesseractException {
        File image = new File("src/main/resources/ali2_invert.jpg");
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("src/main/resources/tessdata");
        tesseract.setLanguage("eng");
//        tesseract.setPageSegMode(3);
//        tesseract.setOcrEngineMode(3);
//        String result = tesseract.doOCR(image, new Rectangle(860,420,160,80));
        String result = tesseract.doOCR(image);
        try {
            Charset charset = Charset.forName("UTF-8");
            InputStreamFactory isf = new InputStreamFactory() {
                public InputStream createInputStream() throws IOException {
                    return new FileInputStream("src/main/resources/tessdata/mobygames.train");
                }
            };
            ObjectStream<String> lineStream = new PlainTextByLineStream(isf, charset);
            ObjectStream<NameSample> sampleStream = new NameSampleDataStream(lineStream);
            TokenNameFinderModel model;
            TokenNameFinderFactory nameFinderFactory = new TokenNameFinderFactory();

            try {
                model = NameFinderME.train("en", "person", sampleStream,
                        TrainingParameters.defaultParams(), nameFinderFactory);
            } finally {
                sampleStream.close();
            }
            OutputStream modelOut=null;
            try {
                modelOut = new BufferedOutputStream(new FileOutputStream("src/main/resources/tessdata/en-ner-person.train"));
                model.serialize(modelOut);
            } finally {
                if (modelOut != null)
                    modelOut.close();
            }
        } catch (Throwable e) {
            System.out.println("???????????");
        }
        System.out.println(result);
    }
}
