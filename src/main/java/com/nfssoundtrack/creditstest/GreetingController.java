package com.nfssoundtrack.creditstest;

import com.itextpdf.text.html.HtmlEncoder;
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
import java.util.stream.Stream;

@Controller
public class GreetingController {

    private static final Logger logger = LoggerFactory.getLogger(GreetingController.class);
    private final SessionHolder sessionHolder;

    @Autowired
    public GreetingController(SessionHolder sessionHolder) {
        this.sessionHolder = sessionHolder;
    }

    @SuppressWarnings("unused")
    @GetMapping("/greeting")
    public String greeting() {
        return "greeting";
    }

    @SuppressWarnings("unused")
    @PostMapping("/fullcredits")
    @ResponseBody
    public ResponseEntity<Map<String, String>> getFullCreditsText(HttpSession session,
                                                                  @RequestParam(value = "pageSegMode", required = false) Integer segMode,
                                                                  @RequestParam(value = "ocrEngineMode", required = false) Integer ocrEngineMode,
                                                                  @RequestParam(value = "lineSeparator", required = false) String lineSeparator,
                                                                  @RequestParam(value = "tempPropX", required = false) String propX,
                                                                  @RequestParam(value = "tempPropY", required = false) String propY,
                                                                  @RequestParam(value = "tempPropW", required = false) String propW,
                                                                  @RequestParam(value = "tempPropH", required = false) String propH,
                                                                  @RequestParam(value = "black") boolean isBlack,
                                                                  @RequestParam(value = "replaceComma") boolean replaceComma,
                                                                  @RequestParam(value = "groupItAll") boolean groupItAll,
                                                                  @RequestParam(value = "twoWordNames") boolean twoWordNames,
                                                                  @RequestParam(value = "nicknameDetect") boolean nicknameDetect,
                                                                  @RequestParam(value = "capitalizeDevNames") boolean capitalizeDevNames,
                                                                  @RequestParam(value = "capitalizeRoles") boolean capitalizeRoles,
                                                                  @RequestParam(value = "uppercaseKeywords", required = false) String uppercaseKeywords,
                                                                  @RequestParam(value = "defineLanguage", required = false) String defineLanguage,
                                                                  @RequestParam(value = "roleDevLayout") Integer roleDevLayout) {
        if (logger.isDebugEnabled()) {
            logger.debug("starting getFullCreditsText with segMode " + segMode + ", ocrEngineMode " + ocrEngineMode
                    + ", propX " + propX + ", propY " + propY + ", propW " + propW + ", propH " + propH +
                    ", isBlack " + isBlack + ", replaceComma " + replaceComma + ", groupItAll " + groupItAll
                    + ", twoWordNames " + twoWordNames + ", nicknameDetect " + nicknameDetect + ", roleDevLayout " + roleDevLayout);
        }
        Map<String, String> allResults = null;
        try {
            if (sessionHolder.getSessionToResults() == null) {
                sessionHolder.setSessionToResults(new HashMap<>());
            }
            ResultsHolder resultsHolder = sessionHolder.getSessionToResults().get(session.getId());
            if (resultsHolder != null) {
                resultsHolder.setResultsPerFile(null);
                resultsHolder.setAllFiles(null);
            } else {
                resultsHolder = new ResultsHolder();
                sessionHolder.getSessionToResults().put(session.getId(), resultsHolder);
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
            tesseractInstance.setLanguage(defineLanguage);
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
            Map<Path, String> resultsPerFile = new TreeMap<>();
            try (Stream<Path> allPaths = Files.list(pathToFolder)) {
                List<Path> allFiles = allPaths.collect(Collectors.toList());
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
                resultsHolder.setResultsPerFile(resultsPerFile);
                resultsHolder.performReading(tesseractInstance, segMode, ocrEngineMode,
                        rectangle, replaceComma, defineLanguage);
                allFiles = null;
            }
            rectangle = null;
            tesseractInstance = null;
            pathToFolder = null;
            if (groupItAll) {
                if (roleDevLayout == 0) {
                    allResults = MobygamesHelper.reworkResultDevUnder(resultsPerFile, nicknameDetect,
                            capitalizeDevNames, capitalizeRoles, uppercaseKeywords);
                } else if (roleDevLayout == 1) {
                    allResults = MobygamesHelper.reworkResultDevNext(resultsPerFile, twoWordNames, nicknameDetect,
                            capitalizeDevNames, capitalizeRoles, uppercaseKeywords, true, lineSeparator);
                } else if (roleDevLayout == 2) {
                    allResults = MobygamesHelper.reworkResultDevNext(resultsPerFile, twoWordNames, nicknameDetect,
                            capitalizeDevNames, capitalizeRoles, uppercaseKeywords, false, lineSeparator);
                } else if (roleDevLayout == 3) {
                    allResults = MobygamesHelper.reworkResultDevNextFulLCredits(resultsPerFile, twoWordNames, nicknameDetect,
                            capitalizeDevNames, capitalizeRoles, uppercaseKeywords, false, lineSeparator);
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
            logger.error(thr.getMessage(), thr);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(allResults);
        }
    }

    @SuppressWarnings("unused")
    @PostMapping("/testupload")
    @ResponseBody
    public ResponseEntity<String> getTextFromImage(HttpSession session,
                                                   @RequestParam("filename") String filename,
                                                   @RequestParam(value = "pageSegMode", required = false) Integer segMode,
                                                   @RequestParam(value = "ocrEngineMode", required = false) Integer ocrEngineMode,
                                                   @RequestParam(value = "lineSeparator", required = false) String lineSeparator,
                                                   @RequestParam(value = "tempPropX", required = false) String propX,
                                                   @RequestParam(value = "tempPropY", required = false) String propY,
                                                   @RequestParam(value = "tempPropW", required = false) String propW,
                                                   @RequestParam(value = "tempPropH", required = false) String propH,
                                                   @RequestParam(value = "black") boolean isBlack,
                                                   @RequestParam(value = "replaceComma") boolean replaceComma,
                                                   @RequestParam(value = "groupItAll") boolean groupItAll,
                                                   @RequestParam(value = "twoWordNames") boolean twoWordNames,
                                                   @RequestParam(value = "nicknameDetect") boolean nicknameDetect,
                                                   @RequestParam(value = "capitalizeDevNames") boolean capitalizeDevNames,
                                                   @RequestParam(value = "capitalizeRoles") boolean capitalizeRoles,
                                                   @RequestParam(value = "uppercaseKeywords", required = false) String uppercaseKeywords,
                                                   @RequestParam(value = "defineLanguage", required = false) String defineLanguage,
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
                    rectangle, img, defineLanguage);
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
            Map<String, String> allResults;
            if (groupItAll) {
                if (roleDevLayout == 0) {
                    allResults = MobygamesHelper.reworkResultDevUnder(resultsPerFile, nicknameDetect,
                            capitalizeDevNames, capitalizeRoles, uppercaseKeywords);
                } else if (roleDevLayout == 1) {
                    allResults = MobygamesHelper.reworkResultDevNext(resultsPerFile, twoWordNames, nicknameDetect,
                            capitalizeDevNames, capitalizeRoles, uppercaseKeywords, true, lineSeparator);
                } else if (roleDevLayout == 2) {
                    allResults = MobygamesHelper.reworkResultDevNext(resultsPerFile, twoWordNames, nicknameDetect,
                            capitalizeDevNames, capitalizeRoles, uppercaseKeywords, false, lineSeparator);
                } else {
                    allResults = new HashMap<>();
                }
                String fileIdName = null;
                for (Map.Entry<Path, String> entry : resultsPerFile.entrySet()) {
                    fileIdName = entry.getKey().getFileName().toString();
                    fileIdName = fileIdName.replace("invert_", "");
                    fileIdName = fileIdName.substring(0, fileIdName.indexOf("."));
                    allResults.put(fileIdName, HtmlEncoder.encode(entry.getValue()));
                }
                if (logger.isDebugEnabled()) {
                    logger.debug("return to page with grouping " + result2);
                }
                return ResponseEntity.status(HttpStatus.OK).body(allResults.get(fileIdName));
            } else {
                return ResponseEntity.status(HttpStatus.OK).body(HtmlEncoder.encode(result2));
            }
        } catch (Throwable thr) {
            logger.error(thr.getMessage(), thr);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(thr.getMessage());
        }
    }

    @SuppressWarnings("unused")
    @PostMapping("/inittestupload")
    @ResponseBody
    public ResponseEntity<String> uploadImageToBackend(HttpSession session,
                                                       @RequestParam("files") MultipartFile[] files,
                                                       @RequestParam(value = "black") boolean isBlack,
                                                       @RequestParam(value = "multiplier") String sizeMultiplier,
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
            if (sessionHolder.getSessionToUploads() == null) {
                sessionHolder.setSessionToUploads(new HashMap<>());
            }
            UploadHolder uploadHolder = sessionHolder.getSessionToUploads().get(session.getId());
            if (uploadHolder != null) {
                uploadHolder.setUploadedFiles(0);
                uploadHolder.setAllFiles(files.length);
            } else {
                uploadHolder = new UploadHolder();
                uploadHolder.setUploadedFiles(0);
                uploadHolder.setAllFiles(files.length);
                sessionHolder.getSessionToUploads().put(session.getId(), uploadHolder);
            }
            UploadHolder finalUploadHolder = uploadHolder;
            Arrays.stream(files).forEach(file -> {
                try {
                    if (file.getOriginalFilename() != null) {
                        Path targetPath = subPath.resolve(file.getOriginalFilename());
                        if (targetPath.toFile().exists()) {
                            boolean delete = targetPath.toFile().delete();
                        }
                        Files.copy(file.getInputStream(), targetPath);
                        finalUploadHolder.setUploadedFiles(finalUploadHolder.getUploadedFiles() + 1);
                        int imageResolutionMultiplier = Integer.parseInt(sizeMultiplier);
                        NameModelHelper.resizeAndInvertImage(targetPath, file.getOriginalFilename(),
                                grayScale, isBlack, imageResolutionMultiplier);
//                        if (isBlack) {
//                            NameModelHelper.invertImage(targetPath, file.getOriginalFilename(), grayScale);
//                        }
                    }
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                    throw new RuntimeException(e);
                }
            });
            return ResponseEntity.status(HttpStatus.OK).body("ok");
        } catch (Throwable thr) {
            logger.error(thr.getMessage(), thr);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(thr.getMessage());
        }
    }

    @SuppressWarnings("unused")
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

    @SuppressWarnings("unused")
    @GetMapping("/getstatus")
    @ResponseBody
    public ResponseEntity<AbstractMap.Entry<String, String>> getTextFromImage(HttpSession session) {
        if (sessionHolder.getSessionToResults() != null) {
            return ResponseEntity.status(HttpStatus.OK).body(
                    sessionHolder.getSessionToResults().get(session.getId()).giveFeedback());
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(
                    new AbstractMap.SimpleEntry<>("ocr", "Starting the process..."));
        }
    }

    @SuppressWarnings("unused")
    @GetMapping("/getuploadstatus")
    @ResponseBody
    public ResponseEntity<AbstractMap.Entry<String, String>> getUploadStatus(HttpSession session) {
        if (sessionHolder.getSessionToUploads() != null) {
            if (logger.isDebugEnabled()) {
                logger.debug("session id " + session.getId());
            }
            UploadHolder idHolder = sessionHolder.getSessionToUploads().get(session.getId());
            if (idHolder != null) {
                return ResponseEntity.status(HttpStatus.OK).body(idHolder.giveFeedback());
            } else {
                return ResponseEntity.status(HttpStatus.OK).body(
                        new AbstractMap.SimpleEntry<>("ocr", "Weird, upload is going on but this message " +
                                "should not appear, please wait"));
            }
        } else {
            return ResponseEntity.status(HttpStatus.OK).body(
                    new AbstractMap.SimpleEntry<>("ocr", "Starting the process..."));
        }
    }

    public String getTextFromImageNoRest(Tesseract tesseractInstance, Integer segMode,
                                         Integer ocrEngineMode, Rectangle rectangle, BufferedImage img,
                                         String language)
            throws URISyntaxException, TesseractException {
        if (logger.isDebugEnabled()) {
            logger.debug("starting cleanSessionImagesDirectory with " + tesseractInstance
                    + ", segMode" + segMode + ", ocrEngineMode " + ocrEngineMode
                    + ", rectangle " + rectangle + ", img " + img);
        }
        if (tesseractInstance == null) {
            tesseractInstance = new Tesseract();
            NameModelHelper.setTesseractDatapath(tesseractInstance);
            tesseractInstance.setLanguage(language);
            if (segMode != null) {
                tesseractInstance.setPageSegMode(segMode);
            }
            if (ocrEngineMode != null) {
                tesseractInstance.setOcrEngineMode(ocrEngineMode);
            }
        }
        String result = tesseractInstance.doOCR(img,null, Collections.singletonList(rectangle));
        if (logger.isDebugEnabled()) {
            logger.debug("OCR done successfully");
        }
        return result;
    }

}