package com.rolling.api.global.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SlackMessageFormatterTest {

    private final SlackMessageFormatter formatter = new SlackMessageFormatter();

    @Test
    @DisplayName("Slack 메시지 포맷은 핵심 필드를 한 줄씩 포함한다")
    void format_includesRequiredFields() {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("lastSummary", "processed=0");

        OperationalAlert alert = OperationalAlert.builder()
                .severity(AlertSeverity.CRITICAL)
                .type(AlertType.SCHEDULER_FAILURE)
                .source("withdrawalProcessor")
                .summary("Scheduler failure")
                .environment("prod")
                .occurredAt(Instant.parse("2026-04-09T01:02:03Z"))
                .schedulerName("withdrawalProcessor")
                .status("FAILED")
                .exceptionClass("IllegalStateException")
                .exceptionMessage("withdraw failed")
                .details(details)
                .build();

        String formatted = formatter.format(alert);

        assertThat(formatted).contains("[ROLLING API][PROD][CRITICAL] Scheduler failure");
        assertThat(formatted).contains("- source: withdrawalProcessor");
        assertThat(formatted).contains("- scheduler: withdrawalProcessor");
        assertThat(formatted).contains("- exception: IllegalStateException");
        assertThat(formatted).contains("- message: withdraw failed");
        assertThat(formatted).contains("- lastSummary: processed=0");
    }
}
