package com.rolling.api.domain.openmat.event;

import com.rolling.api.domain.notification.model.PushNotificationType;

public record OpenMatCommentNotificationEvent(
        Long openMatId,
        Long recipientUserId,
        Long actorUserId,
        String actorNickname,
        String openMatTitle,
        PushNotificationType type
) {
}
