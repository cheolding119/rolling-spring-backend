package com.rolling.api.global.alert;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SlackAlertDispatcher {

    private final SlackWebhookClient slackWebhookClient;
    private final SlackMessageFormatter slackMessageFormatter;
    private final AlertDeduplicator alertDeduplicator;

    @Async("slackAlertTaskExecutor")
    @EventListener
    public void handle(OperationalAlertEvent event) {
        OperationalAlert alert = event.alert();

        if (!alertDeduplicator.shouldSend(alert)) {
            log.debug("Slack alert suppressed by cooldown. type={}, source={}", alert.getType(), alert.getSource());
            return;
        }

        try {
            slackWebhookClient.send(slackMessageFormatter.format(alert));
        } catch (Exception exception) {
            log.warn(
                    "Slack alert dispatch failed. type={}, source={}, reason={}",
                    alert.getType(),
                    alert.getSource(),
                    exception.getMessage()
            );
            log.debug("Slack alert dispatch failure stack trace", exception);
        }
    }
}
