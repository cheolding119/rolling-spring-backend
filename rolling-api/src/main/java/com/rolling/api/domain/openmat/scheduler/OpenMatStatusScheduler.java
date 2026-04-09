package com.rolling.api.domain.openmat.scheduler;

import com.rolling.api.domain.openmat.service.OpenMatService;
import com.rolling.api.global.alert.OperationalAlertPublisher;
import com.rolling.api.global.monitoring.MonitoringTaskNames;
import com.rolling.api.global.monitoring.ScheduledTaskSnapshot;
import com.rolling.api.global.monitoring.ScheduledTaskTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "openmat.status.schedule",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class OpenMatStatusScheduler {

    private final OpenMatService openMatService;
    private final ScheduledTaskTracker scheduledTaskTracker;
    private final OperationalAlertPublisher operationalAlertPublisher;

    @Scheduled(
            cron = "${openmat.status.schedule.cron:0 * * * * *}",
            zone = "${openmat.status.schedule.zone:Asia/Seoul}"
    )
    public void syncExpiredStatuses() {
        log.info("OpenMat status sync started");
        scheduledTaskTracker.recordStart(MonitoringTaskNames.OPEN_MAT_STATUS_SYNC);
        try {
            int synchronizedCount = openMatService.syncExpiredOpenMats();
            scheduledTaskTracker.recordSuccess(
                    MonitoringTaskNames.OPEN_MAT_STATUS_SYNC,
                    "synchronized=" + synchronizedCount
            );
            log.info("OpenMat status sync finished: synchronized={}", synchronizedCount);
        } catch (Exception e) {
            scheduledTaskTracker.recordFailure(MonitoringTaskNames.OPEN_MAT_STATUS_SYNC, e);
            ScheduledTaskSnapshot snapshot = scheduledTaskTracker.snapshot(MonitoringTaskNames.OPEN_MAT_STATUS_SYNC);
            operationalAlertPublisher.publishSchedulerFailure(
                    MonitoringTaskNames.OPEN_MAT_STATUS_SYNC,
                    snapshot.lastSummary(),
                    e
            );
            log.error("OpenMat status sync failed", e);
        }
    }
}
