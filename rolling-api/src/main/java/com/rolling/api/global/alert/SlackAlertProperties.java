package com.rolling.api.global.alert;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "slack.alert")
public class SlackAlertProperties {

    private boolean enabled = false;
    private String webhookUrl = "";
    private String environment = "local";
    private int connectTimeoutMs = 1000;
    private int readTimeoutMs = 3000;
    private int defaultCooldownSeconds = 300;
    private int schedulerCooldownSeconds = 600;
    private boolean notifyUnexpectedException = true;
    private boolean notifyExternalApiException = true;
    private boolean notifySchedulerFailure = true;
    private boolean notifyStartupHealthDown = true;
}
