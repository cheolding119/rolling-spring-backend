package com.rolling.api.global.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
@EnableConfigurationProperties(SlackAlertProperties.class)
public class SlackAlertConfig {

    @Bean
    public SlackWebhookClient slackWebhookClient(SlackAlertProperties properties) {
        if (!properties.isEnabled()) {
            log.info("Slack alerting is disabled. Using NoOpSlackWebhookClient.");
            return new NoOpSlackWebhookClient();
        }

        if (!StringUtils.hasText(properties.getWebhookUrl())) {
            log.warn("Slack alerting is enabled but webhook URL is blank. Using NoOpSlackWebhookClient.");
            return new NoOpSlackWebhookClient();
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());

        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        return new RestClientSlackWebhookClient(restClient, properties.getWebhookUrl());
    }

    @Bean(name = "slackAlertTaskExecutor")
    public Executor slackAlertTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("slack-alert-");
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(100);
        executor.initialize();
        return executor;
    }
}
