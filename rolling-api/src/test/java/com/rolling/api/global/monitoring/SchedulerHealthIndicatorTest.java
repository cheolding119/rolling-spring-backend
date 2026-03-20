package com.rolling.api.global.monitoring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

import static org.assertj.core.api.Assertions.assertThat;

class SchedulerHealthIndicatorTest {

    @Test
    @DisplayName("스케줄러 실패가 마지막 성공보다 최신이면 health는 DOWN이다")
    void healthIsDownWhenLatestExecutionFailed() {
        ScheduledTaskTracker tracker = new ScheduledTaskTracker();
        SchedulerHealthIndicator indicator = new SchedulerHealthIndicator(
                tracker,
                true,
                "0 * * * * *",
                "Asia/Seoul",
                true,
                "0 0 2 * * *",
                "Asia/Seoul",
                "0 * * * * *",
                "Asia/Seoul"
        );

        tracker.recordStart(MonitoringTaskNames.OPEN_MAT_STATUS_SYNC);
        tracker.recordFailure(MonitoringTaskNames.OPEN_MAT_STATUS_SYNC, new IllegalStateException("sync failed"));

        assertThat(indicator.health().getStatus()).isEqualTo(Status.DOWN);
    }

    @Test
    @DisplayName("최근 실행이 모두 성공이면 health는 UP이다")
    void healthIsUpWhenAllEnabledSchedulersSucceeded() {
        ScheduledTaskTracker tracker = new ScheduledTaskTracker();
        SchedulerHealthIndicator indicator = new SchedulerHealthIndicator(
                tracker,
                true,
                "0 * * * * *",
                "Asia/Seoul",
                true,
                "0 0 2 * * *",
                "Asia/Seoul",
                "0 * * * * *",
                "Asia/Seoul"
        );

        tracker.recordStart(MonitoringTaskNames.OPEN_MAT_STATUS_SYNC);
        tracker.recordSuccess(MonitoringTaskNames.OPEN_MAT_STATUS_SYNC, "synchronized=3");
        tracker.recordStart(MonitoringTaskNames.TOURNAMENT_CRAWLER);
        tracker.recordSuccess(MonitoringTaskNames.TOURNAMENT_CRAWLER, "crawled=10");
        tracker.recordStart(MonitoringTaskNames.WITHDRAWAL_PROCESSOR);
        tracker.recordSuccess(MonitoringTaskNames.WITHDRAWAL_PROCESSOR, "processed=0");

        assertThat(indicator.health().getStatus()).isEqualTo(Status.UP);
        assertThat(indicator.health().getDetails()).containsKeys("openMatStatusSync", "tournamentCrawler", "withdrawalProcessor");
    }
}

