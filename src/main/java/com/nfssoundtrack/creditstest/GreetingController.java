package com.nfssoundtrack.creditstest;

import com.lowagie.text.html.HtmlEncoder;
import jakarta.servlet.http.HttpSession;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class GreetingController {

    private static final Logger logger = LoggerFactory.getLogger(GreetingController.class);
    private SessionHolder sessionHolder;

    @Autowired
    public GreetingController(SessionHolder sessionHolder){
        this.sessionHolder=sessionHolder;
    }

    @GetMapping("/greeting")
    public String greeting() {
        return "greeting";
    }

    @PostMapping("/fullcredits")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getFullCreditsText(HttpSession session,
                                                                  @RequestParam(value = "pageSegMode", required = false) Integer segMode,
                                                                  @RequestParam(value = "ocrEngineMode", required = false) Integer ocrEngineMode,
                                                                  @RequestParam(value = "tempPropX", required = false) String propX,
                                                                  @RequestParam(value = "tempPropY", required = false) String propY,
                                                                  @RequestParam(value = "tempPropW", required = false) String propW,
                                                                  @RequestParam(value = "tempPropH", required = false) String propH,
                                                                  @RequestParam(value = "black") boolean isBlack,
                                                                  @RequestParam(value = "replaceComma") boolean replaceComma,
                                                                  @RequestParam(value = "groupItAll") boolean groupItAll,
                                                                  @RequestParam(value = "twoWordNames") boolean twoWordNames,
                                                                  @RequestParam(value = "nicknameDetect") boolean nicknameDetect,
                                                                  @RequestParam(value = "roleDevLayout") Integer roleDevLayout) {
        if (logger.isDebugEnabled()) {
            logger.debug("starting getFullCreditsText with segMode " + segMode + ", ocrEngineMode " + ocrEngineMode
                    + ", propX " + propX + ", propY " + propY + ", propW " + propW + ", propH " + propH +
                    ", isBlack " + isBlack + ", replaceComma " + replaceComma + ", groupItAll " + groupItAll
                    + ", twoWordNames " + twoWordNames + ", nicknameDetect " + nicknameDetect + ", roleDevLayout " + roleDevLayout);
        }
        Map<String, String> allResults = null;
        try {
            if (sessionHolder.getSessionToResults()==null){
                sessionHolder.setSessionToResults(new HashMap<>());
            }
            ResultsHolder resultsHolder = sessionHolder.getSessionToResults().get(session.getId());
            if (resultsHolder!=null){
                resultsHolder.setResultsPerFile(null);
                resultsHolder.setAllFiles(null);
            } else {
                resultsHolder = new ResultsHolder();
                sessionHolder.getSessionToResults().put(session.getId(),resultsHolder);
            }
            Rectangle rectangle = null;
            if (propX != null) {
                rectangle = new Rectangle(Integer.parseInt(propX), Integer.parseInt(propY),
                        Integer.parseInt(propW), Integer.parseInt(propH));
            }
            if (logger.isDebugEnabled()) {
                logger.debug("rectangle created");
            }
            Tesseract tesseractInstance = new Tesseract();
            NameModelHelper.setTesseractDatapath(tesseractInstance);
            tesseractInstance.setLanguage("eng");
            if (segMode != null) {
                tesseractInstance.setPageSegMode(segMode);
            }
            if (ocrEngineMode != null) {
                tesseractInstance.setOcrEngineMode(ocrEngineMode);
            }
            Path pathToFolder = Paths.get("uploads" + File.separator + session.getId() + File.separator);
            if (logger.isDebugEnabled()) {
                logger.debug("path to folder " + pathToFolder);
            }
            List<Path> allFiles = Files.list(pathToFolder).collect(Collectors.toList());
            if (isBlack) {
                allFiles = allFiles.stream().filter(path -> path.getFileName().toString().contains("invert"))
                        .sorted(Comparator.comparing(Object::toString))
                        .collect(Collectors.toList());
            } else {
                allFiles = allFiles.stream().filter(path -> !path.getFileName().toString().contains("invert"))
                        .sorted(Comparator.comparing(Object::toString))
                        .collect(Collectors.toList());
            }
            resultsHolder.setAllFiles(allFiles);
            if (logger.isDebugEnabled()) {
                logger.debug("allFiles size " + allFiles.size());
            }
            Map<Path, String> resultsPerFile = new TreeMap<>();
            resultsHolder.setResultsPerFile(resultsPerFile);
            resultsHolder.performReading(tesseractInstance,segMode,ocrEngineMode,rectangle,replaceComma);
            rectangle=null;
            tesseractInstance=null;
            allFiles=null;
            pathToFolder=null;
            if (groupItAll) {
                if (roleDevLayout == 0) {
                    allResults = MobygamesHelper.reworkResultDevUnder(resultsPerFile, nicknameDetect);
                } else if (roleDevLayout == 1) {
                    allResults = MobygamesHelper.reworkResultDevNext(resultsPerFile, twoWordNames, nicknameDetect);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("returning to page");
                }
                resultsPerFile = null;
                return ResponseEntity.status(HttpStatus.OK).body(allResults);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("not grouping the result");
                }
                allResults = new TreeMap<>();
                for (Map.Entry<Path, String> entry : resultsPerFile.entrySet()) {
                    String fileIdName = entry.getKey().getFileName().toString();
                    fileIdName = fileIdName.replace("invert_", "");
                    fileIdName = fileIdName.substring(0, fileIdName.indexOf("."));
                    allResults.put(fileIdName, HtmlEncoder.encode(entry.getValue()));
                }
            }
            resultsPerFile = null;
            return ResponseEntity.status(HttpStatus.OK).body(allResults);
        } catch (Throwable thr) {
            allResults = new HashMap<>();
            allResults.put(thr.getMessage(), Arrays.toString(thr.getStackTrace()));
            thr.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(allResults);
        }
    }

    @PostMapping("/testupload")
    @ResponseBody
    public ResponseEntity<String> getTextFromImage(HttpSession session,
                                                   @RequestParam("filename") String filename,
                                                   @RequestParam(value = "pageSegMode", required = false) Integer segMode,
                                                   @RequestParam(value = "ocrEngineMode", required = false) Integer ocrEngineMode,
                                                   @RequestParam(value = "tempPropX", required = false) String propX,
                                                   @RequestParam(value = "tempPropY", required = false) String propY,
                                                   @RequestParam(value = "tempPropW", required = false) String propW,
                                                   @RequestParam(value = "tempPropH", required = false) String propH,
                                                   @RequestParam(value = "black") boolean isBlack,
                                                   @RequestParam(value = "replaceComma") boolean replaceComma,
                                                   @RequestParam(value = "groupItAll") boolean groupItAll,
                                                   @RequestParam(value = "twoWordNames") boolean twoWordNames,
                                                   @RequestParam(value = "nicknameDetect") boolean nicknameDetect,
                                                   @RequestParam(value = "roleDevLayout") Integer roleDevLayout
    ) {
        if (logger.isDebugEnabled()) {
            logger.debug("starting getFullCreditsText with segMode " + segMode + ", ocrEngineMode " + ocrEngineMode
                    + ", propX " + propX + ", propY " + propY + ", propW " + propW + ", propH " + propH +
                    ", isBlack " + isBlack + ", replaceComma " + replaceComma + ", groupItAll " + groupItAll
                    + ", twoWordNames " + twoWordNames + ", nicknameDetect " + nicknameDetect + ", roleDevLayout " + roleDevLayout);
        }
        try {
            Rectangle rectangle = null;
            if (propX != null) {
                rectangle = new Rectangle(Integer.parseInt(propX), Integer.parseInt(propY),
                        Integer.parseInt(propW), Integer.parseInt(propH));
            }
            if (logger.isDebugEnabled()) {
                logger.debug("rectangle created");
            }
            Path pathToFile;
            if (isBlack) {
                pathToFile = Paths.get("uploads" + File.separator + session.getId()
                        + File.separator + "invert_" + filename);
            } else {
                pathToFile = Paths.get("uploads" + File.separator + session.getId()
                        + File.separator + filename);
            }
            byte[] imageBytes = Files.readAllBytes(pathToFile);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (logger.isDebugEnabled()) {
                logger.debug("image fetched");
            }
            String result = getTextFromImageNoRest(null, segMode, ocrEngineMode,
                    rectangle, img);
            img = null;
            imageBytes = null;
            if (logger.isDebugEnabled()) {
                logger.debug("text result " + result);
            }
            //we don't really want to have more than 2 \n going on
            String result2 = result.replaceAll("\n\n\n+", "\n\n");
            if (replaceComma) {
                result2 = NameModelHelper.replaceSomeCharacters(result2);
            }
            Map<Path, String> resultsPerFile = new TreeMap<>();
            resultsPerFile.put(pathToFile, result2);
            Map<String, String> allResults = null;
            if (groupItAll) {
                if (roleDevLayout == 0) {
                    allResults = MobygamesHelper.reworkResultDevUnder(resultsPerFile, nicknameDetect);
                } else if (roleDevLayout == 1) {
                    allResults = MobygamesHelper.reworkResultDevNext(resultsPerFile, twoWordNames, nicknameDetect);
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("return to page with grouping " + result2);
                }
                return ResponseEntity.status(HttpStatus.OK).body(allResults.get(pathToFile));
            } else {
                return ResponseEntity.status(HttpStatus.OK).body(HtmlEncoder.encode(result2));
            }
        } catch (Throwable thr) {
            thr.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(thr.getMessage());
        }
    }

    @PostMapping("/inittestupload")
    @ResponseBody
    public ResponseEntity<String> uploadImageToBackend(HttpSession session,
                                                       @RequestParam("files") MultipartFile[] files,
                                                       @RequestParam(value = "black") boolean isBlack,
                                                       @RequestParam(value = "highContrast") boolean grayScale
    ) {
        if (logger.isDebugEnabled()) {
            logger.debug("start uploadImageToBackend with " + session.getId() + ", files " + Arrays.toString(files)
                    + ", isBlack " + isBlack + ", grayScale " + grayScale);
        }
        try {
            Path subPath = Paths.get("uploads" + File.separator + session.getId());
            if (!subPath.toFile().exists()) {
                Files.createDirectory(subPath);
            }
            Arrays.stream(files).forEach(file -> {
                try {
                    Path targetPath = subPath.resolve(file.getOriginalFilename());
                    if (targetPath.toFile().exists()) {
                        targetPath.toFile().delete();
                    }
                    Files.copy(file.getInputStream(), targetPath);
                    if (isBlack) {
                        NameModelHelper.invertImage(targetPath, file.getOriginalFilename(), grayScale);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            return ResponseEntity.status(HttpStatus.OK).body("ok");
        } catch (Throwable thr) {
            thr.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(thr.getMessage());
        }
    }

    @PostMapping("/cleandir")
    @ResponseBody
    public ResponseEntity<String> cleanSessionImagesDirectory(HttpSession session) {
        if (logger.isDebugEnabled()) {
            logger.debug("starting cleanSessionImagesDirectory with " + session.getId());
        }
        Path subPath = Paths.get("uploads" + File.separator + session.getId());
        if (subPath.toFile().exists()) {
            File folder = subPath.toFile();
            File[] listFiles = folder.listFiles();
            if (listFiles != null) {
                for (File file : listFiles) {
                    if (!file.isDirectory()) {
                        boolean fileDeleted = file.delete();
                        if (logger.isDebugEnabled()) {
                            logger.debug("deleted " + file.getName() + ", " + fileDeleted);
                        }
                    }
                }
            }
        }
        return ResponseEntity.status(HttpStatus.OK).body("done");
    }

    @GetMapping("/getstatus")
    @ResponseBody
    public ResponseEntity<AbstractMap.Entry<String,String>> getTextFromImage(HttpSession session){
        if (sessionHolder.getSessionToResults()!=null){
            return ResponseEntity.status(HttpStatus.OK).body(
                sessionHolder.getSessionToResults().get(session.getId()).giveFeedback());
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(
                    new AbstractMap.SimpleEntry<>("ocr", "Starting the process..."));
        }
    }

    public String getTextFromImageNoRest(Tesseract tesseractInstance, Integer segMode,
                                         Integer ocrEngineMode, Rectangle rectangle, BufferedImage img
    ) throws URISyntaxException, TesseractException {
        if (logger.isDebugEnabled()) {
            logger.debug("starting cleanSessionImagesDirectory with " + tesseractInstance
                    + ", segMode" + segMode + ", ocrEngineMode " + ocrEngineMode
                    + ", rectangle " + rectangle + ", img " + img);
        }
        if (tesseractInstance == null) {
            tesseractInstance = new Tesseract();
            NameModelHelper.setTesseractDatapath(tesseractInstance);
            tesseractInstance.setLanguage("eng");
            if (segMode != null) {
                tesseractInstance.setPageSegMode(segMode);
            }
            if (ocrEngineMode != null) {
                tesseractInstance.setOcrEngineMode(ocrEngineMode);
            }
        }
        String result = tesseractInstance.doOCR(img, rectangle);
        if (logger.isDebugEnabled()) {
            logger.debug("OCR done successfully");
        }
        return result;
    }

}