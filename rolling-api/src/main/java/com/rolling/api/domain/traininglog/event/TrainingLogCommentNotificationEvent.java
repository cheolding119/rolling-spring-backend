package com.rolling.api.domain.traininglog.event;

import com.rolling.api.domain.notification.model.PushNotificationType;

public record TrainingLogCommentNotificationEvent(
        Long entryId,
        Long recipientUserId,
        Long actorUserId,
        String actorNickname,
        String entryTitle,
        PushNotificationType type
) {
}
