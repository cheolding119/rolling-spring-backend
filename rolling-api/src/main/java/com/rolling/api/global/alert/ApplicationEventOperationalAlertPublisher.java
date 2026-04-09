package com.rolling.api.global.alert;

import com.rolling.api.global.logging.LogMdcKeys;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ApplicationEventOperationalAlertPublisher implements OperationalAlertPublisher {

    private static final String GLOBAL_EXCEPTION_SOURCE = "GlobalExceptionHandler";
    private static final String STARTUP_SOURCE = "StartupHealthAlertRunner";

    private final ApplicationEventPublisher applicationEventPublisher;
    private final SlackAlertProperties properties;

    @Override
    public void publishUnexpectedException(Exception exception) {
        if (!shouldPublish(properties.isNotifyUnexpectedException())) {
            return;
        }

        publish(OperationalAlert.builder()
                .severity(AlertSeverity.ERROR)
                .type(AlertType.UNEXPECTED_EXCEPTION)
                .source(GLOBAL_EXCEPTION_SOURCE)
                .summary("Unexpected exception")
                .environment(properties.getEnvironment())
                .occurredAt(Instant.now())
                .requestId(mdcValue(LogMdcKeys.REQUEST_ID))
                .userId(mdcValue(LogMdcKeys.USER_ID))
                .method(mdcValue(LogMdcKeys.METHOD))
                .path(mdcValue(LogMdcKeys.PATH))
                .status(mdcValue(LogMdcKeys.STATUS))
                .errorCode(mdcValue(LogMdcKeys.ERROR_CODE))
                .exceptionClass(exception.getClass().getSimpleName())
                .exceptionMessage(summarize(exception.getMessage()))
                .build());
    }

    @Override
    public void publishExternalApiFailure(Exception exception, String source) {
        if (!shouldPublish(properties.isNotifyExternalApiException())) {
            return;
        }

        publish(OperationalAlert.builder()
                .severity(AlertSeverity.ERROR)
                .type(AlertType.EXTERNAL_API_FAILURE)
                .source(source)
                .summary("External API failure")
                .environment(properties.getEnvironment())
                .occurredAt(Instant.now())
                .requestId(mdcValue(LogMdcKeys.REQUEST_ID))
                .userId(mdcValue(LogMdcKeys.USER_ID))
                .method(mdcValue(LogMdcKeys.METHOD))
                .path(mdcValue(LogMdcKeys.PATH))
                .status(mdcValue(LogMdcKeys.STATUS))
                .errorCode(mdcValue(LogMdcKeys.ERROR_CODE))
                .exceptionClass(exception.getClass().getSimpleName())
                .exceptionMessage(summarize(exception.getMessage()))
                .build());
    }

    @Override
    public void publishSchedulerFailure(String schedulerName, String lastSummary, Exception exception) {
        if (!shouldPublish(properties.isNotifySchedulerFailure())) {
            return;
        }

        Map<String, Object> details = new LinkedHashMap<>();
        if (StringUtils.hasText(lastSummary)) {
            details.put("lastSummary", lastSummary);
        }

        publish(OperationalAlert.builder()
                .severity(AlertSeverity.CRITICAL)
                .type(AlertType.SCHEDULER_FAILURE)
                .source(schedulerName)
                .summary("Scheduler failure")
                .environment(properties.getEnvironment())
                .occurredAt(Instant.now())
                .status("FAILED")
                .schedulerName(schedulerName)
                .exceptionClass(exception.getClass().getSimpleName())
                .exceptionMessage(summarize(exception.getMessage()))
                .details(details)
                .build());
    }

    @Override
    public void publishStartupHealthDown(Map<String, Object> healthDetails) {
        if (!shouldPublish(properties.isNotifyStartupHealthDown())) {
            return;
        }

        publish(OperationalAlert.builder()
                .severity(AlertSeverity.CRITICAL)
                .type(AlertType.STARTUP_HEALTH_DOWN)
                .source(STARTUP_SOURCE)
                .summary("Startup health down")
                .environment(properties.getEnvironment())
                .occurredAt(Instant.now())
                .details(new LinkedHashMap<>(healthDetails))
                .build());
    }

    private void publish(OperationalAlert alert) {
        applicationEventPublisher.publishEvent(new OperationalAlertEvent(alert));
    }

    private boolean shouldPublish(boolean typeEnabled) {
        return properties.isEnabled() && typeEnabled;
    }

    private String mdcValue(String key) {
        String value = MDC.get(key);
        if (!StringUtils.hasText(value) || LogMdcKeys.DEFAULT_VALUE.equals(value)) {
            return null;
        }
        return value;
    }

    private String summarize(String message) {
        if (!StringUtils.hasText(message)) {
            return null;
        }

        String trimmed = message.trim();
        if (trimmed.length() <= 200) {
            return trimmed;
        }

        return trimmed.substring(0, 197) + "...";
    }
}
