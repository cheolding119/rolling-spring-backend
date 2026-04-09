package com.rolling.api.global.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AlertDeduplicatorTest {

    @Test
    @DisplayName("동일 fingerprint 알림은 cooldown 내에서 중복 전송되지 않는다")
    void shouldSend_suppressesDuplicateWithinCooldown() {
        SlackAlertProperties properties = new SlackAlertProperties();
        properties.setDefaultCooldownSeconds(300);
        properties.setSchedulerCooldownSeconds(600);
        AlertDeduplicator deduplicator = new AlertDeduplicator(properties);

        OperationalAlert alert = OperationalAlert.builder()
                .severity(AlertSeverity.ERROR)
                .type(AlertType.UNEXPECTED_EXCEPTION)
                .source("GlobalExceptionHandler")
                .summary("Unexpected exception")
                .status("500")
                .errorCode("INTERNAL_ERROR")
                .path("/api/v1/open-mats")
                .exceptionClass("NullPointerException")
                .build();

        assertThat(deduplicator.shouldSend(alert)).isTrue();
        assertThat(deduplicator.shouldSend(alert)).isFalse();
    }
}
