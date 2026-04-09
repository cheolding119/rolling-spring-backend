package com.rolling.api.global.alert;

public interface SlackWebhookClient {

    void send(String text);
}
