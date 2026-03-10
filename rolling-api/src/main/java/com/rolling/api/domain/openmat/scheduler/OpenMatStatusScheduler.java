package com.rolling.api.domain.openmat.scheduler;

import com.rolling.api.domain.openmat.service.OpenMatService;
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

    @Scheduled(
            cron = "${openmat.status.schedule.cron:0 * * * * *}",
            zone = "${openmat.status.schedule.zone:Asia/Seoul}"
    )
    public void syncExpiredStatuses() {
        log.info("OpenMat status sync started");
        try {
            int synchronizedCount = openMatService.syncExpiredOpenMats();
            log.info("OpenMat status sync finished: synchronized={}", synchronizedCount);
        } catch (Exception e) {
            log.error("OpenMat status sync failed", e);
        }
    }
}
