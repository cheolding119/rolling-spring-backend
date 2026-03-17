package com.rolling.api.domain.notification.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "push-notification")
public record PushNotificationProperties(
        String androidChannelId
) {

    private static final String DEFAULT_ANDROID_CHANNEL_ID = "rolling_open_mat_alerts";

    public PushNotificationProperties {
        androidChannelId = StringUtils.hasText(androidChannelId)
                ? androidChannelId
                : DEFAULT_ANDROID_CHANNEL_ID;
    }
}
