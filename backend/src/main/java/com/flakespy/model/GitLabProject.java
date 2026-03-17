package com.flakespy.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "gitlab_projects")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GitLabProject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String projectId;           // GitLab project ID (found in Settings → General)

    @Column(nullable = false)
    private String projectName;         // e.g. "my-mobile-app"

    @Column(nullable = false)
    private String personalAccessToken; // read_api scoped token

    @Column(nullable = false)
    private String jobName;             // CI job name that produces XML e.g. "run-tests"

    @Column(nullable = false)
    private String gitlabUrl;           // https://gitlab.com or self-hosted URL

    private LocalDateTime lastSyncedAt; // when FlakeSpy last pulled artifacts

    private boolean active;             // soft disable without deleting

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        active = true;
    }
}
