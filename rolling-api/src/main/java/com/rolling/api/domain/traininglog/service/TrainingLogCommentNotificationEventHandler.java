package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
import com.rolling.api.domain.traininglog.event.TrainingLogCommentNotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class TrainingLogCommentNotificationEventHandler {

    private static final String TRAINING_LOG_DETAIL_ROUTE = "/training-logs/friends/entries/";

    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TrainingLogCommentNotificationEvent event) {
        PushNotificationCommand command = new PushNotificationCommand(
                event.type(),
                titleFor(event.type()),
                bodyFor(event),
                event.entryId(),
                Map.of("route", TRAINING_LOG_DETAIL_ROUTE + event.entryId())
        );

        try {
            notificationService.saveNotificationsForUsers(List.of(event.recipientUserId()), command);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist training log comment notification inbox. entryId={}", event.entryId(), exception);
            return;
        }

        try {
            pushNotificationService.sendToUsers(List.of(event.recipientUserId()), command);
        } catch (RuntimeException exception) {
            log.warn("Failed to send training log comment push notification. entryId={}", event.entryId(), exception);
        }
    }

    private String titleFor(PushNotificationType type) {
        return switch (type) {
            case TRAINING_LOG_COMMENT_CREATED -> "훈련일지에 새 댓글이 달렸습니다";
            case TRAINING_LOG_COMMENT_REPLY_CREATED -> "훈련일지 댓글에 새 답글이 달렸습니다";
            default -> "훈련일지 알림";
        };
    }

    private String bodyFor(TrainingLogCommentNotificationEvent event) {
        return switch (event.type()) {
            case TRAINING_LOG_COMMENT_CREATED ->
                    event.actorNickname() + "님이 \"" + event.entryTitle() + "\" 기록에 댓글을 남겼습니다.";
            case TRAINING_LOG_COMMENT_REPLY_CREATED ->
                    event.actorNickname() + "님이 \"" + event.entryTitle() + "\" 기록 댓글에 답글을 남겼습니다.";
            default -> event.actorNickname() + "님이 훈련일지에 반응했습니다.";
        };
    }
}
