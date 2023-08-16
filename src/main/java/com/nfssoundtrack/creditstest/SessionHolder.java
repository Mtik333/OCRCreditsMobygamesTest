package com.nfssoundtrack.creditstest;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SessionHolder {

    private Map<String, ResultsHolder> sessionToResults;
    private Map<String, UploadHolder> sessionToUploads;

    public Map<String, ResultsHolder> getSessionToResults() {
        return sessionToResults;
    }

    public void setSessionToResults(Map<String, ResultsHolder> sessionToResults) {
        this.sessionToResults = sessionToResults;
    }

    public Map<String, UploadHolder> getSessionToUploads() {
        return sessionToUploads;
    }

    public void setSessionToUploads(Map<String, UploadHolder> sessionToUploads) {
        this.sessionToUploads = sessionToUploads;
    }
}
