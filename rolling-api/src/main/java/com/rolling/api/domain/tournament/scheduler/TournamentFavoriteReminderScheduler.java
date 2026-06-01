package com.rolling.api.domain.tournament.scheduler;

import com.rolling.api.domain.tournament.dto.TournamentFavoriteReminderDispatchResult;
import com.rolling.api.domain.tournament.service.TournamentFavoriteService;
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
        prefix = "tournament.favorite-reminder.schedule",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class TournamentFavoriteReminderScheduler {

    private final TournamentFavoriteService tournamentFavoriteService;
    private final ScheduledTaskTracker scheduledTaskTracker;
    private final OperationalAlertPublisher operationalAlertPublisher;

    @Scheduled(
            cron = "${tournament.favorite-reminder.schedule.cron:0 * * * * *}",
            zone = "${tournament.favorite-reminder.schedule.zone:Asia/Seoul}"
    )
    public void dispatchDueReminders() {
        scheduledTaskTracker.recordStart(MonitoringTaskNames.TOURNAMENT_FAVORITE_REMINDER);
        try {
            TournamentFavoriteReminderDispatchResult result = tournamentFavoriteService.dispatchDueReminders();
            scheduledTaskTracker.recordSuccess(
                    MonitoringTaskNames.TOURNAMENT_FAVORITE_REMINDER,
                    String.format(
                            "scanned=%d,sent=%d,disabled=%d,skipped=%d",
                            result.scannedCount(),
                            result.sentCount(),
                            result.disabledCount(),
                            result.skippedCount()
                    )
            );
        } catch (Exception exception) {
            scheduledTaskTracker.recordFailure(MonitoringTaskNames.TOURNAMENT_FAVORITE_REMINDER, exception);
            ScheduledTaskSnapshot snapshot = scheduledTaskTracker.snapshot(MonitoringTaskNames.TOURNAMENT_FAVORITE_REMINDER);
            operationalAlertPublisher.publishSchedulerFailure(
                    MonitoringTaskNames.TOURNAMENT_FAVORITE_REMINDER,
                    snapshot.lastSummary(),
                    exception
            );
            log.error("Tournament favorite reminder scheduler failed", exception);
        }
    }
}
