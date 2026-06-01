package com.rolling.api.global.monitoring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component("scheduler")
public class SchedulerHealthIndicator implements HealthIndicator {

    private final ScheduledTaskTracker scheduledTaskTracker;
    private final boolean openMatSchedulerEnabled;
    private final String openMatSchedulerCron;
    private final String openMatSchedulerZone;
    private final boolean tournamentSchedulerEnabled;
    private final String tournamentSchedulerCron;
    private final String tournamentSchedulerZone;
    private final boolean tournamentFavoriteReminderSchedulerEnabled;
    private final String tournamentFavoriteReminderSchedulerCron;
    private final String tournamentFavoriteReminderSchedulerZone;
    private final String withdrawalSchedulerCron;
    private final String withdrawalSchedulerZone;

    public SchedulerHealthIndicator(
            ScheduledTaskTracker scheduledTaskTracker,
            @Value("${openmat.status.schedule.enabled:true}") boolean openMatSchedulerEnabled,
            @Value("${openmat.status.schedule.cron:0 * * * * *}") String openMatSchedulerCron,
            @Value("${openmat.status.schedule.zone:Asia/Seoul}") String openMatSchedulerZone,
            @Value("${tournament.crawler.schedule.enabled:true}") boolean tournamentSchedulerEnabled,
            @Value("${tournament.crawler.schedule.cron:0 0 2 * * *}") String tournamentSchedulerCron,
            @Value("${tournament.crawler.schedule.zone:Asia/Seoul}") String tournamentSchedulerZone,
            @Value("${tournament.favorite-reminder.schedule.enabled:true}") boolean tournamentFavoriteReminderSchedulerEnabled,
            @Value("${tournament.favorite-reminder.schedule.cron:0 * * * * *}") String tournamentFavoriteReminderSchedulerCron,
            @Value("${tournament.favorite-reminder.schedule.zone:Asia/Seoul}") String tournamentFavoriteReminderSchedulerZone,
            @Value("${auth.withdraw.schedule.cron:0 * * * * *}") String withdrawalSchedulerCron,
            @Value("${auth.withdraw.schedule.zone:Asia/Seoul}") String withdrawalSchedulerZone
    ) {
        this.scheduledTaskTracker = scheduledTaskTracker;
        this.openMatSchedulerEnabled = openMatSchedulerEnabled;
        this.openMatSchedulerCron = openMatSchedulerCron;
        this.openMatSchedulerZone = openMatSchedulerZone;
        this.tournamentSchedulerEnabled = tournamentSchedulerEnabled;
        this.tournamentSchedulerCron = tournamentSchedulerCron;
        this.tournamentSchedulerZone = tournamentSchedulerZone;
        this.tournamentFavoriteReminderSchedulerEnabled = tournamentFavoriteReminderSchedulerEnabled;
        this.tournamentFavoriteReminderSchedulerCron = tournamentFavoriteReminderSchedulerCron;
        this.tournamentFavoriteReminderSchedulerZone = tournamentFavoriteReminderSchedulerZone;
        this.withdrawalSchedulerCron = withdrawalSchedulerCron;
        this.withdrawalSchedulerZone = withdrawalSchedulerZone;
    }

    @Override
    public Health health() {
        ScheduledTaskSnapshot openMatSnapshot = scheduledTaskTracker.snapshot(MonitoringTaskNames.OPEN_MAT_STATUS_SYNC);
        ScheduledTaskSnapshot tournamentSnapshot = scheduledTaskTracker.snapshot(MonitoringTaskNames.TOURNAMENT_CRAWLER);
        ScheduledTaskSnapshot tournamentFavoriteReminderSnapshot = scheduledTaskTracker.snapshot(MonitoringTaskNames.TOURNAMENT_FAVORITE_REMINDER);
        ScheduledTaskSnapshot withdrawalSnapshot = scheduledTaskTracker.snapshot(MonitoringTaskNames.WITHDRAWAL_PROCESSOR);

        Health.Builder builder = hasBlockingFailure(openMatSchedulerEnabled, openMatSnapshot)
                || hasBlockingFailure(tournamentSchedulerEnabled, tournamentSnapshot)
                || hasBlockingFailure(tournamentFavoriteReminderSchedulerEnabled, tournamentFavoriteReminderSnapshot)
                || hasBlockingFailure(true, withdrawalSnapshot)
                ? Health.down()
                : Health.up();

        return builder
                .withDetail("openMatStatusSync", toDetail(openMatSnapshot, openMatSchedulerEnabled, openMatSchedulerCron, openMatSchedulerZone))
                .withDetail("tournamentCrawler", toDetail(tournamentSnapshot, tournamentSchedulerEnabled, tournamentSchedulerCron, tournamentSchedulerZone))
                .withDetail("tournamentFavoriteReminder", toDetail(
                        tournamentFavoriteReminderSnapshot,
                        tournamentFavoriteReminderSchedulerEnabled,
                        tournamentFavoriteReminderSchedulerCron,
                        tournamentFavoriteReminderSchedulerZone
                ))
                .withDetail("withdrawalProcessor", toDetail(withdrawalSnapshot, true, withdrawalSchedulerCron, withdrawalSchedulerZone))
                .build();
    }

    private boolean hasBlockingFailure(boolean enabled, ScheduledTaskSnapshot snapshot) {
        if (!enabled) {
            return false;
        }

        if (!"FAILED".equals(snapshot.state())) {
            return false;
        }

        return snapshot.lastSucceededAt() == null
                || (snapshot.lastFailedAt() != null && snapshot.lastFailedAt().isAfter(snapshot.lastSucceededAt()));
    }

    private Map<String, Object> toDetail(ScheduledTaskSnapshot snapshot, boolean enabled, String cron, String zone) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("enabled", enabled);
        detail.put("cron", cron);
        detail.put("zone", zone);
        detail.put("state", snapshot.state());
        detail.put("running", snapshot.running());
        detail.put("executionCount", snapshot.executionCount());
        detail.put("lastStartedAt", snapshot.lastStartedAt());
        detail.put("lastFinishedAt", snapshot.lastFinishedAt());
        detail.put("lastSucceededAt", snapshot.lastSucceededAt());
        detail.put("lastFailedAt", snapshot.lastFailedAt());
        detail.put("lastSummary", snapshot.lastSummary());
        detail.put("lastError", snapshot.lastError());
        return detail;
    }
}

