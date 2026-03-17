package com.flakespy.repository;

import com.flakespy.model.TestResult;
import com.flakespy.model.TestRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TestResultRepository extends JpaRepository<TestResult, Long> {

    // get all results for a specific run
    List<TestResult> findByTestRun(TestRun testRun);

    // get full history for one specific test by name (for trend view)
    List<TestResult> findByTestNameOrderByCreatedAtAsc(String testName);

    // get all FAILED results for a specific test (for AI analysis)
    List<TestResult> findByTestNameAndStatus(String testName, String status);

    // get top 20 flakiest tests for a project — the leaderboard query
    @Query("""
        SELECT tr.testName, tr.className,
               COUNT(tr) as totalRuns,
               SUM(CASE WHEN tr.status = 'FAILED' THEN 1 ELSE 0 END) as failures,
               tr.flakinessScore
        FROM TestResult tr
        JOIN tr.testRun run
        JOIN run.gitLabProject p
        WHERE p.id = :projectId
        AND tr.flakinessScore IS NOT NULL
        ORDER BY tr.flakinessScore DESC
        LIMIT 20
    """)
    List<Object[]> findTopFlakyTestsByProject(@Param("projectId") Long projectId);

    // get all distinct test names for a project (to loop through for scoring)
    @Query("""
        SELECT DISTINCT tr.testName
        FROM TestResult tr
        JOIN tr.testRun run
        JOIN run.gitLabProject p
        WHERE p.id = :projectId
    """)
    List<String> findDistinctTestNamesByProject(@Param("projectId") Long projectId);

    // get last 30 results for a specific test in a project (for flakiness scoring)
    @Query("""
        SELECT tr FROM TestResult tr
        JOIN tr.testRun run
        JOIN run.gitLabProject p
        WHERE p.id = :projectId
        AND tr.testName = :testName
        ORDER BY tr.createdAt DESC
        LIMIT 30
    """)
    List<TestResult> findLast30ResultsForTest(
        @Param("projectId") Long projectId,
        @Param("testName") String testName
    );
}
