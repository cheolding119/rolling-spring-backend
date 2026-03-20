package com.rolling.api.domain.tournament.scheduler;

import com.rolling.api.domain.tournament.dto.TournamentCrawlResult;
import com.rolling.api.domain.tournament.service.TournamentManagerService;
import com.rolling.api.global.monitoring.MonitoringTaskNames;
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
        prefix = "tournament.crawler.schedule",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class TournamentCrawlerScheduler {

    private final TournamentManagerService tournamentManagerService;
    private final ScheduledTaskTracker scheduledTaskTracker;

    @Scheduled(
            cron = "${tournament.crawler.schedule.cron:0 0 2 * * *}",
            zone = "${tournament.crawler.schedule.zone:Asia/Seoul}"
    )
    public void scheduledCrawlAndSave() {
        log.info("Tournament scheduled crawl started");
        scheduledTaskTracker.recordStart(MonitoringTaskNames.TOURNAMENT_CRAWLER);
        try {
            TournamentCrawlResult result = tournamentManagerService.crawlAndSaveAll();
            scheduledTaskTracker.recordSuccess(
                    MonitoringTaskNames.TOURNAMENT_CRAWLER,
                    String.format(
                            "crawled=%d,created=%d,updated=%d,skipped=%d",
                            result.getCrawledCount(),
                            result.getCreatedCount(),
                            result.getUpdatedCount(),
                            result.getSkippedCount()
                    )
            );
            log.info("Tournament scheduled crawl finished: crawled={}, created={}, updated={}, skipped={}",
                    result.getCrawledCount(),
                    result.getCreatedCount(),
                    result.getUpdatedCount(),
                    result.getSkippedCount());
        } catch (Exception e) {
            scheduledTaskTracker.recordFailure(MonitoringTaskNames.TOURNAMENT_CRAWLER, e);
            log.error("Tournament scheduled crawl failed", e);
        }
    }
}
