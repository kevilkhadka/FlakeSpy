package com.flakespy.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "test_runs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // which GitLab project this run belongs to
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "gitlab_project_id", nullable = false)
    private GitLabProject gitLabProject;

    @Column(nullable = false)
    private Long gitlabJobId;           // the GitLab CI job ID

    @Column(nullable = false)
    private String gitlabJobName;       // e.g. "run-tests"

    @Column(nullable = false)
    private String branch;              // e.g. "main" or "develop"

    @Column(nullable = false)
    private LocalDateTime runDate;      // when the nightly pipeline ran

    private int totalTests;             // total number of tests in this run
    private int passed;                 // how many passed
    private int failed;                 // how many failed
    private int skipped;                // how many were skipped

    // all individual test results in this run
    @OneToMany(mappedBy = "testRun", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<TestResult> testResults;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
