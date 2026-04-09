package com.rolling.api.global.alert;

public class NoOpSlackWebhookClient implements SlackWebhookClient {

    @Override
    public void send(String text) {
        // Intentionally no-op when Slack alerting is disabled or unconfigured.
    }
}
