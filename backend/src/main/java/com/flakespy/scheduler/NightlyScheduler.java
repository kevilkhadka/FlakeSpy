package com.flakespy.scheduler;

import com.flakespy.dto.FlakyTestSummary;
import com.flakespy.model.GitLabProject;
import com.flakespy.repository.GitLabProjectRepository;
import com.flakespy.service.FlakinessAnalyserService;
import com.flakespy.service.GitLabService;
import com.flakespy.service.GroqAIService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class NightlyScheduler {

    private final GitLabProjectRepository gitLabProjectRepository;
    private final GitLabService gitLabService;
    private final FlakinessAnalyserService flakinessAnalyserService;
    private final GroqAIService groqAIService;

    // runs every day at 6am
    // cron format: second minute hour day month weekday
    @Scheduled(cron = "${flakespy.scheduler.cron}")
    public void runNightlySync() {
        log.info("===== FlakeSpy nightly sync started at {} =====",
                LocalDateTime.now());

        // get all active connected GitLab projects
        List<GitLabProject> projects = gitLabProjectRepository
                .findByActiveTrue();

        if (projects.isEmpty()) {
            log.info("No active projects connected. Skipping sync.");
            return;
        }

        for (GitLabProject project : projects) {
            try {
                syncProject(project);
            } catch (Exception e) {
                // don't let one project failure stop others
                log.error("Sync failed for project {}: {}",
                        project.getProjectName(), e.getMessage());
            }
        }

        log.info("===== FlakeSpy nightly sync complete at {} =====",
                LocalDateTime.now());
    }

    // full sync pipeline for one project
    public void syncProject(GitLabProject project) {
        log.info("--- Starting sync for: {} ---", project.getProjectName());

        // Step 1 — pull last night's XML artifacts from GitLab
        log.info("[1/3] Pulling artifacts from GitLab...");
        gitLabService.syncProject(project);

        // Step 2 — recalculate flakiness scores for all tests
        log.info("[2/3] Calculating flakiness scores...");
        List<FlakyTestSummary> leaderboard = flakinessAnalyserService
                .buildLeaderboard(project);

        // Step 3 — run AI analysis on flaky tests only (score > 0.2)
        log.info("[3/3] Running AI analysis on flaky tests...");
        List<String> flakyTestNames = leaderboard.stream()
                .filter(t -> t.getFlakinessScore() > 0.2)
                .map(FlakyTestSummary::getTestName)
                .toList();

        if (flakyTestNames.isEmpty()) {
            log.info("No flaky tests found — suite is healthy! ✅");
        } else {
            log.info("Found {} flaky tests — analysing...",
                    flakyTestNames.size());
            groqAIService.analyseAllFlakyTests(project.getId(), flakyTestNames);
        }

        // update last synced timestamp
        project.setLastSyncedAt(LocalDateTime.now());
        gitLabProjectRepository.save(project);

        log.info("--- Sync complete for: {} ---", project.getProjectName());
    }

    // manual trigger — called from GitLabController when user hits "Sync Now"
    public void triggerManualSync(GitLabProject project) {
        log.info("Manual sync triggered for: {}", project.getProjectName());
        syncProject(project);
    }
}
