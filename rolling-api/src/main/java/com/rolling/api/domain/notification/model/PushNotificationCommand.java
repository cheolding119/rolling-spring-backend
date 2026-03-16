package com.rolling.api.domain.notification.model;

import java.util.Map;

public record PushNotificationCommand(
        PushNotificationType type,
        String title,
        String body,
        Long targetId,
        Map<String, String> data
) {

    public PushNotificationCommand {
        data = data == null ? Map.of() : Map.copyOf(data);
    }
}
