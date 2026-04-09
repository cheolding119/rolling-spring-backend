package com.rolling.api.global.alert;

import java.util.Map;

public interface OperationalAlertPublisher {

    void publishUnexpectedException(Exception exception);

    void publishExternalApiFailure(Exception exception, String source);

    void publishSchedulerFailure(String schedulerName, String lastSummary, Exception exception);

    void publishStartupHealthDown(Map<String, Object> healthDetails);
}
