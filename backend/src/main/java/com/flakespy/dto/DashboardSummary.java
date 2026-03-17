package com.flakespy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummary {

    private String projectName;

    // latest nightly run stats
    private int totalTests;
    private int passed;
    private int failed;
    private int skipped;

    // suite health score (0-100)
    // 100 = all tests stable, 0 = everything is broken
    private int healthScore;
    private String healthLabel;         // "Healthy", "Degraded", "Critical"

    // flakiness overview
    private int totalFlakyTests;        // tests with score > 0.5
    private int totalBrokenTests;       // tests with score > 0.8
    private int totalStableTests;       // tests with score <= 0.2

    // when data was last updated
    private LocalDateTime lastSyncedAt;
    private LocalDateTime lastRunDate;

    // convenience method
    public String getHealthLabel() {
        if (healthScore >= 80) return "Healthy";
        if (healthScore >= 50) return "Degraded";
        return "Critical";
    }
}
