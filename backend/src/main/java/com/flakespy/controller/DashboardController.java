package com.flakespy.controller;

import com.flakespy.dto.DashboardSummary;
import com.flakespy.dto.FlakyTestSummary;
import com.flakespy.model.GitLabProject;
import com.flakespy.model.TestRun;
import com.flakespy.repository.GitLabProjectRepository;
import com.flakespy.repository.TestRunRepository;
import com.flakespy.service.FlakinessAnalyserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Slf4j
public class DashboardController {

    private final GitLabProjectRepository gitLabProjectRepository;
    private final TestRunRepository testRunRepository;
    private final FlakinessAnalyserService flakinessAnalyserService;

    // GET /api/dashboard/summary?projectId=1
    // returns overall suite health for the dashboard header
    @GetMapping("/summary")
    public ResponseEntity<?> getSummary(@RequestParam Long projectId) {

        Optional<GitLabProject> projectOpt =
                gitLabProjectRepository.findById(projectId);

        if (projectOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        GitLabProject project = projectOpt.get();

        // get the most recent nightly run
        Optional<TestRun> latestRun = testRunRepository
                .findTopByGitLabProjectOrderByRunDateDesc(project);

        if (latestRun.isEmpty()) {
            return ResponseEntity.ok(DashboardSummary.builder()
                    .projectName(project.getProjectName())
                    .healthScore(0)
                    .lastSyncedAt(project.getLastSyncedAt())
                    .build());
        }

        TestRun run = latestRun.get();

        // get flakiness breakdown
        List<FlakyTestSummary> leaderboard =
                flakinessAnalyserService.buildLeaderboard(project);

        long stable  = leaderboard.stream()
                .filter(t -> t.getFlakinessScore() <= 0.2).count();
        long flaky   = leaderboard.stream()
                .filter(t -> t.getFlakinessScore() > 0.2
                          && t.getFlakinessScore() <= 0.8).count();
        long broken  = leaderboard.stream()
                .filter(t -> t.getFlakinessScore() > 0.8).count();

        // health score formula:
        // start at 100, subtract 5 per flaky test, 15 per broken test
        int healthScore = (int) Math.max(0,
                100 - (flaky * 5) - (broken * 15));

        DashboardSummary summary = new DashboardSummary();
        summary.setProjectName(project.getProjectName());
        summary.setTotalTests(run.getTotalTests());
        summary.setPassed(run.getPassed());
        summary.setFailed(run.getFailed());
        summary.setSkipped(run.getSkipped());
        summary.setHealthScore(healthScore);
        summary.setTotalStableTests((int) stable);
        summary.setTotalFlakyTests((int) flaky);
        summary.setTotalBrokenTests((int) broken);
        summary.setLastSyncedAt(project.getLastSyncedAt());
        summary.setLastRunDate(run.getRunDate());

        return ResponseEntity.ok(summary);
    }
}
