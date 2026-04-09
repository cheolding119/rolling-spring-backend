package com.rolling.api.global.alert;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class SlackMessageFormatter {

    private static final String APP_NAME = "ROLLING API";

    public String format(OperationalAlert alert) {
        List<String> lines = new ArrayList<>();
        lines.add(String.format(
                "[%s][%s][%s] %s",
                APP_NAME,
                normalizeEnvironment(alert.getEnvironment()),
                alert.getSeverity().name(),
                alert.getSummary()
        ));

        addLine(lines, "source", alert.getSource());
        addLine(lines, "occurredAt", alert.getOccurredAt());
        addLine(lines, "requestId", alert.getRequestId());
        addLine(lines, "userId", alert.getUserId());
        addLine(lines, "method", alert.getMethod());
        addLine(lines, "path", alert.getPath());
        addLine(lines, "status", alert.getStatus());
        addLine(lines, "errorCode", alert.getErrorCode());
        addLine(lines, "scheduler", alert.getSchedulerName());
        addLine(lines, "exception", alert.getExceptionClass());
        addLine(lines, "message", alert.getExceptionMessage());

        for (Map.Entry<String, Object> entry : alert.getDetails().entrySet()) {
            addLine(lines, entry.getKey(), entry.getValue());
        }

        return String.join(System.lineSeparator(), lines);
    }

    private String normalizeEnvironment(String environment) {
        if (!StringUtils.hasText(environment)) {
            return "LOCAL";
        }
        return environment.trim().toUpperCase();
    }

    private void addLine(List<String> lines, String label, Object value) {
        if (value == null) {
            return;
        }

        String rendered = String.valueOf(value).trim();
        if (!StringUtils.hasText(rendered) || "-".equals(rendered)) {
            return;
        }

        lines.add("- " + label + ": " + rendered);
    }
}
