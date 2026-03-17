package com.flakespy.repository;

import com.flakespy.model.GitLabProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GitLabProjectRepository extends JpaRepository<GitLabProject, Long> {

    // find a project by its GitLab project ID
    Optional<GitLabProject> findByProjectId(String projectId);

    // get all active connected projects (used by NightlyScheduler)
    List<GitLabProject> findByActiveTrue();

    // check if a project is already connected
    boolean existsByProjectId(String projectId);
}
