package com.rolling.api.domain.traininglog.event;

import com.rolling.api.domain.notification.model.PushNotificationType;

public record FriendRequestNotificationEvent(
        Long requestId,
        Long senderUserId,
        String senderNickname,
        Long recipientUserId,
        PushNotificationType type
) {
}
