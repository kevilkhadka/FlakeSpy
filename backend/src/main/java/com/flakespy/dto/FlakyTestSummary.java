package com.flakespy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FlakyTestSummary {

    private String testName;            // e.g. "loginWithGoogleOAuthTest"
    private String className;           // e.g. "com.app.tests.AuthTest"

    private int totalRuns;              // how many nightly runs recorded
    private int failures;               // how many times it failed
    private int passes;                 // how many times it passed

    private double flakinessScore;      // 0.0 (stable) → 1.0 (maximally flaky)
    private String flakinessLabel;      // "Stable", "Unstable", "Flaky", "Broken"

    private String lastStatus;          // PASSED or FAILED
    private String lastFailureMessage;  // most recent failure message

    private String rootCause;           // TIMING, LOCATOR, DATA_DEPENDENCY etc.
    private String fixSuggestion;       // AI-generated fix advice

    // convenience method — frontend uses this for colour coding
    public String getFlakinessLabel() {
        if (flakinessScore <= 0.2) return "Stable";
        if (flakinessScore <= 0.5) return "Unstable";
        if (flakinessScore <= 0.8) return "Flaky";
        return "Broken";
    }
}
