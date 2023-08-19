package com.nfssoundtrack.creditstest;

import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import nu.pattern.OpenCV;
import opennlp.tools.namefind.*;
import opennlp.tools.util.InputStreamFactory;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.TrainingParameters;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class Ble {

    public static void main(String[] args) throws TesseractException, IOException {
        File image = new File("src/main/resources/out002.jpeg");
        File image2 = new File("src/main/resources/out002_result.jpeg");
        OpenCV.loadLocally();
//        BufferedImage original = ImageIO.read(image);
//        BufferedImage imageCopy = new BufferedImage(original.getWidth(), original.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
//        imageCopy.getGraphics().drawImage(original,0,0,null);
//        byte[] data = ((DataBufferByte) imageCopy.getRaster().getDataBuffer()).getData();
        Mat img = Imgcodecs.imread("src/main/resources/out002.jpeg");
        Mat gray = new Mat();
        Mat thresh1 = new Mat();
        Imgproc.cvtColor(img,gray,Imgproc.COLOR_BGR2GRAY);
        Imgproc.threshold(gray,thresh1, 0, 150, Imgproc.THRESH_OTSU | Imgproc.THRESH_BINARY_INV);
        //Imgproc.adaptiveThreshold(gray, thresh1, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, 15, 40);
        Imgcodecs.imwrite("src/main/resources/out002_result.jpeg", thresh1);
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath("src/main/resources/tessdata");
        tesseract.setLanguage("eng");
//        tesseract.setPageSegMode(3);
//        tesseract.setOcrEngineMode(3);
//        String result = tesseract.doOCR(image, new Rectangle(860,420,160,80));
        String result = tesseract.doOCR(image);
        try {
            Charset charset = StandardCharsets.UTF_8;
            InputStreamFactory isf = () -> new FileInputStream("src/main/resources/tessdata/mobygames.train");
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
            try (OutputStream modelOut = new BufferedOutputStream(new FileOutputStream("src/main/resources/tessdata/en-ner-person.train"))) {
                model.serialize(modelOut);
            }
        } catch (Throwable e) {
            System.out.println("???????????");
        }
        System.out.println(result);
    }
}
