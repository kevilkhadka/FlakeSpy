package com.flakespy.controller;

import com.flakespy.dto.GitLabConnectRequest;
import com.flakespy.model.GitLabProject;
import com.flakespy.repository.GitLabProjectRepository;
import com.flakespy.scheduler.NightlyScheduler;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gitlab")
@RequiredArgsConstructor
@Slf4j
public class GitLabController {

    private final GitLabProjectRepository gitLabProjectRepository;
    private final NightlyScheduler nightlyScheduler;

    // POST /api/gitlab/connect
    // called when user fills in the GitLab connect form
    @PostMapping("/connect")
    public ResponseEntity<?> connect(
            @Valid @RequestBody GitLabConnectRequest request) {

        // check if project already connected
        if (gitLabProjectRepository.existsByProjectId(request.getProjectId())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error",
                        "Project already connected. Use sync to refresh."));
        }

        // save the new project
        GitLabProject project = new GitLabProject();
        project.setProjectId(request.getProjectId());
        project.setProjectName(request.getProjectName());
        project.setPersonalAccessToken(request.getPersonalAccessToken());
        project.setJobName(request.getJobName());
        project.setGitlabUrl(request.getGitlabUrl());

        GitLabProject saved = gitLabProjectRepository.save(project);

        log.info("New GitLab project connected: {}", saved.getProjectName());

        // trigger immediate first sync so user sees data straight away
        nightlyScheduler.triggerManualSync(saved);

        return ResponseEntity.ok(Map.of(
            "message", "Project connected and first sync started!",
            "projectId", saved.getId()
        ));
    }

    // POST /api/gitlab/sync/{id}
    // manual sync trigger — user clicks "Sync Now" button
    @PostMapping("/sync/{id}")
    public ResponseEntity<?> syncNow(@PathVariable Long id) {
        return gitLabProjectRepository.findById(id)
                .map(project -> {
                    log.info("Manual sync triggered for: {}",
                            project.getProjectName());
                    nightlyScheduler.triggerManualSync(project);
                    return ResponseEntity.ok(Map.of(
                        "message", "Sync started for " + project.getProjectName()
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    // GET /api/gitlab/projects
    // returns all connected projects
    @GetMapping("/projects")
    public ResponseEntity<?> getProjects() {
        return ResponseEntity.ok(
            gitLabProjectRepository.findByActiveTrue()
                .stream()
                .map(p -> Map.of(
                    "id",            p.getId(),
                    "projectName",   p.getProjectName(),
                    "projectId",     p.getProjectId(),
                    "jobName",       p.getJobName(),
                    "lastSyncedAt",  p.getLastSyncedAt() != null
                                        ? p.getLastSyncedAt().toString()
                                        : "Never"
                ))
                .toList()
        );
    }

    // DELETE /api/gitlab/projects/{id}
    // disconnect a project
    @DeleteMapping("/projects/{id}")
    public ResponseEntity<?> disconnect(@PathVariable Long id) {
        return gitLabProjectRepository.findById(id)
                .map(project -> {
                    project.setActive(false);
                    gitLabProjectRepository.save(project);
                    return ResponseEntity.ok(Map.of(
                        "message", project.getProjectName() + " disconnected"
                    ));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
