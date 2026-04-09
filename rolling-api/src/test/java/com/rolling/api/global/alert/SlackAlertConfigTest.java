package com.rolling.api.global.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SlackAlertConfigTest {

    private final SlackAlertConfig slackAlertConfig = new SlackAlertConfig();

    @Test
    @DisplayName("enabled=false 이면 no-op webhook client를 사용한다")
    void slackWebhookClient_whenDisabled_returnsNoOp() {
        SlackAlertProperties properties = new SlackAlertProperties();
        properties.setEnabled(false);

        SlackWebhookClient client = slackAlertConfig.slackWebhookClient(properties);

        assertThat(client).isInstanceOf(NoOpSlackWebhookClient.class);
    }

    @Test
    @DisplayName("webhook URL이 비어 있으면 no-op webhook client를 사용한다")
    void slackWebhookClient_whenWebhookMissing_returnsNoOp() {
        SlackAlertProperties properties = new SlackAlertProperties();
        properties.setEnabled(true);
        properties.setWebhookUrl(" ");

        SlackWebhookClient client = slackAlertConfig.slackWebhookClient(properties);

        assertThat(client).isInstanceOf(NoOpSlackWebhookClient.class);
    }
}
