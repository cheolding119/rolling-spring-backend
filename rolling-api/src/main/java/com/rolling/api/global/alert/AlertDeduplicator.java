package com.rolling.api.global.alert;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class AlertDeduplicator {

    private final SlackAlertProperties properties;
    private final Map<String, Instant> lastSentAt = new ConcurrentHashMap<>();

    public boolean shouldSend(OperationalAlert alert) {
        String fingerprint = buildFingerprint(alert);
        Duration cooldown = resolveCooldown(alert);
        Instant now = Instant.now();
        boolean[] shouldSend = {false};

        lastSentAt.compute(fingerprint, (key, lastSent) -> {
            if (lastSent == null || now.isAfter(lastSent.plus(cooldown))) {
                shouldSend[0] = true;
                return now;
            }
            return lastSent;
        });

        return shouldSend[0];
    }

    private Duration resolveCooldown(OperationalAlert alert) {
        if (alert.getType() == AlertType.SCHEDULER_FAILURE) {
            return Duration.ofSeconds(properties.getSchedulerCooldownSeconds());
        }
        return Duration.ofSeconds(properties.getDefaultCooldownSeconds());
    }

    private String buildFingerprint(OperationalAlert alert) {
        return String.join("|",
                value(alert.getType()),
                value(alert.getSource()),
                value(alert.getStatus()),
                value(alert.getErrorCode()),
                value(alert.getPath()),
                value(alert.getSchedulerName()),
                value(alert.getExceptionClass())
        );
    }

    private String value(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
