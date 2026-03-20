package com.rolling.api.global.monitoring;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class MonitoringTaskNames {

    public static final String OPEN_MAT_STATUS_SYNC = "openMatStatusSync";
    public static final String TOURNAMENT_CRAWLER = "tournamentCrawler";
    public static final String WITHDRAWAL_PROCESSOR = "withdrawalProcessor";
}
