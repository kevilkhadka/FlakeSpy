package com.flakespy.controller;

import com.flakespy.dto.FlakyTestSummary;
import com.flakespy.dto.TestAnalysis;
import com.flakespy.model.GitLabProject;
import com.flakespy.model.TestResult;
import com.flakespy.repository.GitLabProjectRepository;
import com.flakespy.repository.TestResultRepository;
import com.flakespy.service.FlakinessAnalyserService;
import com.flakespy.service.GroqAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tests")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    private final GitLabProjectRepository gitLabProjectRepository;
    private final TestResultRepository testResultRepository;
    private final FlakinessAnalyserService flakinessAnalyserService;
    private final GroqAIService groqAIService;

    // GET /api/tests/flaky?projectId=1
    // returns the flakiness leaderboard for a project
    @GetMapping("/flaky")
    public ResponseEntity<?> getFlakyTests(
            @RequestParam Long projectId) {

        return gitLabProjectRepository.findById(projectId)
                .map(project -> {
                    List<FlakyTestSummary> leaderboard =
                            flakinessAnalyserService.buildLeaderboard(project);
                    return ResponseEntity.ok(leaderboard);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/tests/{testName}/history?projectId=1
    // returns 30-night pass/fail history for one test
    @GetMapping("/{testName}/history")
    public ResponseEntity<?> getTestHistory(
            @PathVariable String testName,
            @RequestParam Long projectId) {

        List<TestResult> history = testResultRepository
                .findLast30ResultsForTest(projectId, testName);

        if (history.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // map to lightweight response
        List<TestAnalysis.NightlyResult> nightlyResults = history.stream()
                .map(r -> new TestAnalysis.NightlyResult(
                    r.getCreatedAt(),
                    r.getStatus(),
                    r.getDuration(),
                    r.getFailureMessage()
                ))
                .toList();

        return ResponseEntity.ok(nightlyResults);
    }

    // GET /api/tests/{testName}/analysis?projectId=1
    // returns full AI analysis for one test
    @GetMapping("/{testName}/analysis")
    public ResponseEntity<?> getTestAnalysis(
            @PathVariable String testName,
            @RequestParam Long projectId) {

        List<TestResult> results = testResultRepository
                .findLast30ResultsForTest(projectId, testName);

        if (results.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // trigger fresh AI analysis if not yet done
        TestResult latest = results.get(0);
        if (latest.getRootCause() == null) {
            groqAIService.analyseFlakyTest(testName, projectId);
            // reload after analysis
            results = testResultRepository
                    .findLast30ResultsForTest(projectId, testName);
            latest = results.get(0);
        }

        // build full analysis response
        FlakyTestSummary summary = flakinessAnalyserService
                .analyseTest(gitLabProjectRepository.findById(projectId)
                        .orElseThrow(), testName);

        List<TestAnalysis.NightlyResult> history = results.stream()
                .map(r -> new TestAnalysis.NightlyResult(
                    r.getCreatedAt(),
                    r.getStatus(),
                    r.getDuration(),
                    r.getFailureMessage()
                ))
                .toList();

        TestAnalysis analysis = new TestAnalysis();
        analysis.setTestName(testName);
        analysis.setClassName(latest.getClassName());
        analysis.setFlakinessScore(summary != null
                ? summary.getFlakinessScore() : 0.0);
        analysis.setTotalRuns(summary != null ? summary.getTotalRuns() : 0);
        analysis.setFailures(summary != null ? summary.getFailures() : 0);
        analysis.setPasses(summary != null ? summary.getPasses() : 0);
        analysis.setRootCause(latest.getRootCause());
        analysis.setFixSuggestion(latest.getFixSuggestion());
        analysis.setHistory(history);
        analysis.setMostCommonFailureMessage(latest.getFailureMessage());

        return ResponseEntity.ok(analysis);
    }
}
