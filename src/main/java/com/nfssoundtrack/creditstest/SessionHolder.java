package com.nfssoundtrack.creditstest;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SessionHolder {

    private Map<String,ResultsHolder> sessionToResults;

    public Map<String, ResultsHolder> getSessionToResults() {
        return sessionToResults;
    }

    public void setSessionToResults(Map<String, ResultsHolder> sessionToResults) {
        this.sessionToResults = sessionToResults;
    }
}
