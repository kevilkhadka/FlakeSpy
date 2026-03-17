package com.flakespy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestAnalysis {

    private String testName;
    private String className;

    // flakiness stats
    private double flakinessScore;
    private String flakinessLabel;
    private int totalRuns;
    private int failures;
    private int passes;

    // AI root cause
    private String rootCause;           // TIMING, LOCATOR, DATA_DEPENDENCY, NETWORK, CONCURRENCY, ENVIRONMENT
    private String rootCauseExplanation;// human-readable explanation of why
    private String fixSuggestion;       // specific fix advice

    // failure pattern observed
    private String failurePattern;      // e.g. "Fails consistently between 2am-3am"

    // 30-night history for trend chart
    private List<NightlyResult> history;

    // most common failure message across all runs
    private String mostCommonFailureMessage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NightlyResult {
        private LocalDateTime date;     // which night
        private String status;          // PASSED, FAILED, SKIPPED
        private double duration;        // how long it took in seconds
        private String failureMessage;  // null if passed
    }
}
