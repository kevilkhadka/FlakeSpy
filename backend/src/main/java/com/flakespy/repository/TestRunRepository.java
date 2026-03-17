package com.flakespy.repository;

import com.flakespy.model.TestRun;
import com.flakespy.model.GitLabProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TestRunRepository extends JpaRepository<TestRun, Long> {

    // get all runs for a project, newest first
    List<TestRun> findByGitLabProjectOrderByRunDateDesc(GitLabProject gitLabProject);

    // get last N runs for a project (used for flakiness calculation)
    List<TestRun> findTop30ByGitLabProjectOrderByRunDateDesc(GitLabProject gitLabProject);

    // check if we already pulled a specific GitLab job (avoid duplicates)
    boolean existsByGitlabJobId(Long gitlabJobId);

    // get runs between two dates (for trend charts)
    List<TestRun> findByGitLabProjectAndRunDateBetweenOrderByRunDateAsc(
        GitLabProject gitLabProject,
        LocalDateTime from,
        LocalDateTime to
    );

    // get the most recent run for a project
    Optional<TestRun> findTopByGitLabProjectOrderByRunDateDesc(GitLabProject gitLabProject);
}
