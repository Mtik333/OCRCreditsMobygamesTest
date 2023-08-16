package com.nfssoundtrack.creditstest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;

public class UploadHolder {

    private final Logger logger = LoggerFactory.getLogger(UploadHolder.class);

    private int uploadedFiles;
    private int allFiles;

    public int getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(int uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public int getAllFiles() {
        return allFiles;
    }

    public void setAllFiles(int allFiles) {
        this.allFiles = allFiles;
    }

    public AbstractMap.Entry<String, String> giveFeedback() {
        if (uploadedFiles != 0) {
            int inputSize = getUploadedFiles();
            int finalSize = getAllFiles();
            if (inputSize != finalSize) {
                return new AbstractMap.SimpleEntry<>("ocr", "Uploaded images: "
                        + inputSize + "/" + finalSize);
            } else return new AbstractMap.SimpleEntry<>("ocr", "Finishing upload...");
        } else {
            return new AbstractMap.SimpleEntry<>("ocr", "Process has just begun...");
        }
    }
}
