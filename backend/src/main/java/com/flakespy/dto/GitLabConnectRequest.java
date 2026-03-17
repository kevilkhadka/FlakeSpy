package com.flakespy.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GitLabConnectRequest {

    @NotBlank(message = "GitLab project ID is required")
    private String projectId;           // found in GitLab → Settings → General

    @NotBlank(message = "Project name is required")
    private String projectName;         // e.g. "my-mobile-app"

    @NotBlank(message = "Personal access token is required")
    private String personalAccessToken; // read_api scoped token

    @NotBlank(message = "CI job name is required")
    private String jobName;             // e.g. "run-tests"

    private String gitlabUrl = "https://gitlab.com"; // default to gitlab.com
}
