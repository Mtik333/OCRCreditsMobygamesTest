package com.nfssoundtrack.creditstest;

import com.lowagie.text.html.HtmlEncoder;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpSession;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class GreetingController {

    private final Path root = Paths.get("uploads");

    @GetMapping("/greeting")
    public String greeting(@RequestParam(name = "name", required = false, defaultValue = "World") String name, Model model) {
        model.addAttribute("name", name);
        return "greeting";
    }

    @PostMapping("/fullcredits")
    @ResponseBody
    public Map<String,String> getFullCreditsText(HttpSession session,
                                     @RequestParam(value = "pageSegMode", required = false) Integer segMode,
                                     @RequestParam(value = "ocrEngineMode", required = false) Integer ocrEngineMode,
                                     @RequestParam(value = "tempPropX", required = false) String propX,
                                     @RequestParam(value = "tempPropY", required = false) String propY,
                                     @RequestParam(value = "tempPropW", required = false) String propW,
                                     @RequestParam(value = "tempPropH", required = false) String propH,
                                     @RequestParam(value = "black") boolean isBlack,
                                                 @RequestParam(value = "roleDevLayout") Integer roleDevLayout)
            throws IOException, TesseractException {
        System.out.println("???????????????");
        Rectangle rectangle = null;
        if (propX != null) {
            rectangle = new Rectangle(Integer.parseInt(propX), Integer.parseInt(propY),
                    Integer.parseInt(propW), Integer.parseInt(propH));
        }
        Tesseract tesseractInstance = new Tesseract();
        tesseractInstance.setDatapath("src/main/resources/tessdata");
        tesseractInstance.setLanguage("eng");
        if (segMode != null) {
            tesseractInstance.setPageSegMode(segMode);
        }
        if (ocrEngineMode != null) {
            tesseractInstance.setOcrEngineMode(ocrEngineMode);
        }
        Path pathToFolder = Paths.get("uploads" + File.separator + session.getId() + File.separator);
//        File folderPath = pathToFolder.toFile();
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
        Map<Path, String> resultsPerFile = new TreeMap<>();
        for (Path path : allFiles) {
            byte[] imageBytes = Files.readAllBytes(path);
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(imageBytes));
            String textResult = getTextFromImageNoRest(tesseractInstance, segMode, ocrEngineMode,
                    rectangle, img);
            String result2 = textResult.replaceAll("\n\n\n+", "\n\n");
            String result3 = NameModelHelper.replaceSomeCharacters(result2);
            resultsPerFile.put(path, result3);
        }
        Map<String,String> allResults=null;
        if (roleDevLayout==0){
            allResults = MobygamesHelper.reworkResultDevUnder(resultsPerFile);
        } else if (roleDevLayout==1){
            allResults = MobygamesHelper.reworkResultDevNext(resultsPerFile);
        }
        return allResults;
    }

    @PostMapping("/testupload")
    @ResponseBody
    public String getTextFromImage(HttpSession session,
                                   @RequestParam("filename") String filename,
                                   @RequestParam(value = "pageSegMode", required = false) Integer segMode,
                                   @RequestParam(value = "ocrEngineMode", required = false) Integer ocrEngineMode,
                                   @RequestParam(value = "tempPropX", required = false) String propX,
                                   @RequestParam(value = "tempPropY", required = false) String propY,
                                   @RequestParam(value = "tempPropW", required = false) String propW,
                                   @RequestParam(value = "tempPropH", required = false) String propH,
                                   @RequestParam(value = "black") boolean isBlack,
                                   @RequestParam(value = "roleDevLayout") Integer roleDevLayout
    )

            throws IOException, TesseractException {
        System.out.println("???????????????");
        Rectangle rectangle = null;
        if (propX != null) {
            rectangle = new Rectangle(Integer.parseInt(propX), Integer.parseInt(propY),
                    Integer.parseInt(propW), Integer.parseInt(propH));
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
        String result = getTextFromImageNoRest(null, segMode, ocrEngineMode,
                rectangle, img);
        //we don't really want to have more than 2 \n going on
        String result2 = result.replaceAll("\n\n\n+", "\n\n");
        String result3 = NameModelHelper.replaceSomeCharacters(result2);
        Map<Path, String> resultsPerFile = new TreeMap<>();
        resultsPerFile.put(pathToFile,result3);
        Map<String,String> allResults=null;
        if (roleDevLayout==0){
            allResults = MobygamesHelper.reworkResultDevUnder(resultsPerFile);
        } else if (roleDevLayout==1){
            allResults = MobygamesHelper.reworkResultDevNext(resultsPerFile);
        }
//        NameModelHelper.analyzeSentence(result);
        return allResults.get(pathToFile);
    }

    @PostMapping("/inittestupload")
    @ResponseBody
    public String uploadImageToBackend(HttpSession session,
                                       @RequestParam("files") MultipartFile[] files,
                                       @RequestParam(value = "black") boolean isBlack
    ) throws IOException {
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
                    NameModelHelper.invertImage(targetPath, file.getOriginalFilename());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return "tttt";
    }

    @PostMapping("/cleandir")
    @ResponseBody
    public String cleanSessionImagesDirectory(HttpSession session) {
        Path subPath = Paths.get("uploads" + File.separator + session.getId());
        if (subPath.toFile().exists()) {
            File folder = subPath.toFile();
            for (File file : folder.listFiles()) {
                if (!file.isDirectory()) {
                    file.delete();
                }
            }
        }
        return "done";
    }

    public String getTextFromImageNoRest(Tesseract tesseractInstance, Integer segMode,
                                         Integer ocrEngineMode, Rectangle rectangle, BufferedImage img
    ) throws TesseractException {
        if (tesseractInstance == null) {
            tesseractInstance = new Tesseract();
            tesseractInstance.setDatapath("src/main/resources/tessdata");
            tesseractInstance.setLanguage("eng");
            if (segMode != null) {
                tesseractInstance.setPageSegMode(segMode);
            }
            if (ocrEngineMode != null) {
                tesseractInstance.setOcrEngineMode(ocrEngineMode);
            }
        }
        String result = tesseractInstance.doOCR(img, rectangle);
        //we don't really want to have more than 2 \n going on
//        String result2 = result.replaceAll("\n\n\n+", "\n\n");
//        String result3 = NameModelHelper.replaceSomeCharacters(result2);
//        NameModelHelper.analyzeSentence(result);
        return result;
    }

//    @Autowired
//    FilesStorageService storageService;
//
//    @PostMapping("/upload")
//    public ResponseEntity<ResponseMessage> uploadFiles(@RequestParam("files") MultipartFile[] files) {
//        String message = "";
//        try {
//            List<String> fileNames = new ArrayList<>();
//
//            Arrays.asList(files).stream().forEach(file -> {
//                storageService.save(file);
//                fileNames.add(file.getOriginalFilename());
//            });
//
//            message = "Uploaded the files successfully: " + fileNames;
//            return ResponseEntity.status(HttpStatus.OK).body(new ResponseMessage(message));
//        } catch (Exception e) {
//            message = "Fail to upload files!";
//            return ResponseEntity.status(HttpStatus.EXPECTATION_FAILED).body(new ResponseMessage(message));
//        }
//    }

//    @GetMapping("/files")
//    public ResponseEntity<List<FileInfo>> getListFiles() {
//        List<FileInfo> fileInfos = storageService.loadAll().map(path -> {
//            String filename = path.getFileName().toString();
//            String url = MvcUriComponentsBuilder
//                    .fromMethodName(GreetingController.class, "getFile", path.getFileName().toString()).build().toString();
//
//            return new FileInfo(filename, url);
//        }).collect(Collectors.toList());
//
//        return ResponseEntity.status(HttpStatus.OK).body(fileInfos);
//    }
//
//    @GetMapping("/files/{filename:.+}")
//    public ResponseEntity<Resource> getFile(@PathVariable String filename) {
//        Resource file = storageService.load(filename);
//        return ResponseEntity.ok()
//                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.getFilename() + "\"").body(file);
//    }
}