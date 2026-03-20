package com.rolling.api.global.monitoring;

import java.time.Instant;

public record ScheduledTaskSnapshot(
        String taskName,
        String state,
        boolean running,
        Instant lastStartedAt,
        Instant lastFinishedAt,
        Instant lastSucceededAt,
        Instant lastFailedAt,
        String lastSummary,
        String lastError,
        long executionCount
) {
}
