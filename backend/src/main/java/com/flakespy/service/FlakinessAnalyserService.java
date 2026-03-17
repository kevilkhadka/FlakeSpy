package com.flakespy.service;

import com.flakespy.dto.FlakyTestSummary;
import com.flakespy.model.GitLabProject;
import com.flakespy.model.TestResult;
import com.flakespy.repository.TestResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FlakinessAnalyserService {

    private final TestResultRepository testResultRepository;

    // analyse all tests for a project and return ranked leaderboard
    public List<FlakyTestSummary> buildLeaderboard(GitLabProject project) {
        List<String> testNames = testResultRepository
                .findDistinctTestNamesByProject(project.getId());

        List<FlakyTestSummary> leaderboard = new ArrayList<>();

        for (String testName : testNames) {
            FlakyTestSummary summary = analyseTest(project, testName);

            // only include tests with enough runs to be meaningful
            if (summary != null) {
                leaderboard.add(summary);
            }
        }

        // sort by flakiness score descending — worst tests first
        leaderboard.sort((a, b) ->
                Double.compare(b.getFlakinessScore(), a.getFlakinessScore()));

        log.info("Leaderboard built — {} tests analysed for project {}",
                leaderboard.size(), project.getProjectName());

        return leaderboard;
    }

    // analyse one specific test across its last 30 runs
    public FlakyTestSummary analyseTest(GitLabProject project, String testName) {
        List<TestResult> results = testResultRepository
                .findLast30ResultsForTest(project.getId(), testName);

        // need at least 5 runs to give a meaningful score
        if (results.size() < 5) {
            log.debug("Not enough runs for test: {}", testName);
            return null;
        }

        int totalRuns  = results.size();
        int failures   = 0;
        int passes     = 0;
        int statusFlips = 0;
        String lastStatus = null;
        String lastFailureMessage = null;

        // walk through results oldest → newest
        List<TestResult> chronological = new ArrayList<>(results);
        Collections.reverse(chronological);

        for (TestResult result : chronological) {
            if ("PASSED".equals(result.getStatus())) {
                passes++;
            } else if ("FAILED".equals(result.getStatus())) {
                failures++;
                lastFailureMessage = result.getFailureMessage();
            }

            // count how many times the status flipped
            // e.g. PASS → FAIL → PASS = 2 flips
            if (lastStatus != null
                    && !lastStatus.equals(result.getStatus())
                    && !"SKIPPED".equals(result.getStatus())) {
                statusFlips++;
            }

            if (!"SKIPPED".equals(result.getStatus())) {
                lastStatus = result.getStatus();
            }
        }

        // flakiness score = flips / (total non-skipped runs - 1)
        // 0.0 = always same result (stable)
        // 1.0 = flips every single run (maximally flaky)
        int nonSkipped = passes + failures;
        double score = nonSkipped > 1
                ? (double) statusFlips / (nonSkipped - 1)
                : 0.0;

        // cap at 1.0
        score = Math.min(score, 1.0);

        // save score back to the most recent result for this test
        saveScore(results.get(0), score);

        // build the summary DTO
        FlakyTestSummary summary = new FlakyTestSummary();
        summary.setTestName(testName);
        summary.setClassName(chronological.get(0).getClassName());
        summary.setTotalRuns(totalRuns);
        summary.setFailures(failures);
        summary.setPasses(passes);
        summary.setFlakinessScore(Math.round(score * 100.0) / 100.0);
        summary.setLastStatus(lastStatus);
        summary.setLastFailureMessage(lastFailureMessage);
        summary.setRootCause(results.get(0).getRootCause());
        summary.setFixSuggestion(results.get(0).getFixSuggestion());

        return summary;
    }

    // update flakiness score on the TestResult entity
    private void saveScore(TestResult result, double score) {
        result.setFlakinessScore(score);
        testResultRepository.save(result);
    }

    // convenience — recalculate scores for ALL projects
    // called by NightlyScheduler after syncing
    public void recalculateAllScores(GitLabProject project) {
        log.info("Recalculating flakiness scores for: {}",
                project.getProjectName());
        buildLeaderboard(project);
    }
}
