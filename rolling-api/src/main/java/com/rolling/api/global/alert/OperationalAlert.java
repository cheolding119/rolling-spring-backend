package com.rolling.api.global.alert;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Map;

@Getter
@Builder
public class OperationalAlert {

    private final AlertSeverity severity;
    private final AlertType type;
    private final String source;
    private final String summary;
    private final String environment;
    private final Instant occurredAt;
    private final String requestId;
    private final String userId;
    private final String method;
    private final String path;
    private final String status;
    private final String errorCode;
    private final String schedulerName;
    private final String exceptionClass;
    private final String exceptionMessage;

    @Builder.Default
    private final Map<String, Object> details = Map.of();
}
