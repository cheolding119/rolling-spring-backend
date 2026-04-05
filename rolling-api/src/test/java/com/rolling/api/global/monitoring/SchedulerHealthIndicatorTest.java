package com.rolling.api.global.monitoring;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;
import org.springframework.test.util.ReflectionTestUtils;

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

    @Test
    @DisplayName("스케줄러 성공과 실패는 메트릭으로 기록된다")
    void schedulerMetrics_areRecorded() {
        ScheduledTaskTracker tracker = new ScheduledTaskTracker();
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ReflectionTestUtils.setField(tracker, "meterRegistry", meterRegistry);

        tracker.recordStart(MonitoringTaskNames.TOURNAMENT_CRAWLER);
        tracker.recordSuccess(MonitoringTaskNames.TOURNAMENT_CRAWLER, "crawled=3");
        tracker.recordStart(MonitoringTaskNames.WITHDRAWAL_PROCESSOR);
        tracker.recordFailure(MonitoringTaskNames.WITHDRAWAL_PROCESSOR, new IllegalStateException("failed"));

        assertThat(meterRegistry.get("rolling_scheduler_execution_total")
                .tag("task", MonitoringTaskNames.TOURNAMENT_CRAWLER)
                .tag("result", "success")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("rolling_scheduler_execution_total")
                .tag("task", MonitoringTaskNames.WITHDRAWAL_PROCESSOR)
                .tag("result", "failure")
                .counter()
                .count()).isEqualTo(1.0);
        assertThat(meterRegistry.get("rolling_scheduler_last_success_unixtime")
                .tag("task", MonitoringTaskNames.TOURNAMENT_CRAWLER)
                .gauge()
                .value()).isPositive();
    }
}

