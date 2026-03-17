package com.flakespy.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "test_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // which nightly run this result belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "test_run_id", nullable = false)
    private TestRun testRun;

    @Column(nullable = false)
    private String testName;            // e.g. "loginWithGoogleOAuthTest"

    @Column(nullable = false)
    private String className;           // e.g. "com.app.tests.AuthTest"

    @Column(nullable = false)
    private String status;              // PASSED, FAILED, SKIPPED

    private double duration;            // test execution time in seconds

    @Column(columnDefinition = "TEXT")
    private String failureMessage;      // the failure message if status = FAILED

    @Column(columnDefinition = "TEXT")
    private String stackTrace;          // full stack trace if status = FAILED

    // AI analysis fields — populated by GroqAIService
    private String rootCause;           // TIMING, LOCATOR, DATA_DEPENDENCY, NETWORK, CONCURRENCY, ENVIRONMENT

    @Column(columnDefinition = "TEXT")
    private String fixSuggestion;       // AI-generated fix advice

    private Double flakinessScore;      // 0.0 (stable) → 1.0 (maximally flaky)

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
