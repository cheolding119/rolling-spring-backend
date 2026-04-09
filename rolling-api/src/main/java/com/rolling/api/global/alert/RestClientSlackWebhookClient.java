package com.rolling.api.global.alert;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.Map;

public class RestClientSlackWebhookClient implements SlackWebhookClient {

    private final RestClient restClient;
    private final String webhookUrl;

    public RestClientSlackWebhookClient(RestClient restClient, String webhookUrl) {
        this.restClient = restClient;
        this.webhookUrl = webhookUrl;
    }

    @Override
    public void send(String text) {
        try {
            restClient.post()
                    .uri(webhookUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("text", text))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException exception) {
            throw new IllegalStateException(
                    "Slack webhook request failed with status=" + exception.getStatusCode().value(),
                    exception
            );
        }
    }
}
