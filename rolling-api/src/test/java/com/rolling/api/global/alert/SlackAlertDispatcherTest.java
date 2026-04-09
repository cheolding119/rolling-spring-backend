package com.rolling.api.global.alert;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class SlackAlertDispatcherTest {

    @Test
    @DisplayName("dispatcher는 dedupe를 통과한 알림을 Slack client로 전달한다")
    void handle_dispatchesAlert() {
        SlackWebhookClient slackWebhookClient = mock(SlackWebhookClient.class);
        SlackAlertProperties properties = new SlackAlertProperties();
        AlertDeduplicator deduplicator = new AlertDeduplicator(properties);
        SlackAlertDispatcher dispatcher = new SlackAlertDispatcher(
                slackWebhookClient,
                new SlackMessageFormatter(),
                deduplicator
        );

        dispatcher.handle(new OperationalAlertEvent(testAlert()));

        verify(slackWebhookClient, times(1)).send(org.mockito.ArgumentMatchers.contains("예상하지 못한 서버 오류"));
    }

    @Test
    @DisplayName("dispatcher는 Slack 전송 실패를 재전파하지 않는다")
    void handle_whenSlackFails_doesNotThrow() {
        SlackWebhookClient slackWebhookClient = mock(SlackWebhookClient.class);
        doThrow(new IllegalStateException("slack failed")).when(slackWebhookClient).send(org.mockito.ArgumentMatchers.anyString());
        SlackAlertProperties properties = new SlackAlertProperties();
        AlertDeduplicator deduplicator = new AlertDeduplicator(properties);
        SlackAlertDispatcher dispatcher = new SlackAlertDispatcher(
                slackWebhookClient,
                new SlackMessageFormatter(),
                deduplicator
        );

        assertThatCode(() -> dispatcher.handle(new OperationalAlertEvent(testAlert()))).doesNotThrowAnyException();
    }

    private OperationalAlert testAlert() {
        return OperationalAlert.builder()
                .severity(AlertSeverity.ERROR)
                .type(AlertType.UNEXPECTED_EXCEPTION)
                .source("GlobalExceptionHandler")
                .summary("Unexpected exception")
                .environment("prod")
                .status("500")
                .errorCode("INTERNAL_ERROR")
                .exceptionClass("IllegalStateException")
                .exceptionMessage("boom")
                .build();
    }
}
