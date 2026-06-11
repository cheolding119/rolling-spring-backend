package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
import com.rolling.api.domain.openmat.event.OpenMatCommentNotificationEvent;
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
public class OpenMatCommentNotificationEventHandler {

    private static final String OPEN_MAT_DETAIL_ROUTE = "/openmat/detail";

    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OpenMatCommentNotificationEvent event) {
        PushNotificationCommand command = new PushNotificationCommand(
                event.type(),
                titleFor(event.type()),
                bodyFor(event),
                event.openMatId(),
                Map.of("route", OPEN_MAT_DETAIL_ROUTE)
        );

        try {
            notificationService.saveNotificationsForUsers(List.of(event.recipientUserId()), command);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist open mat comment notification inbox. openMatId={}", event.openMatId(), exception);
            return;
        }

        try {
            pushNotificationService.sendToUsers(List.of(event.recipientUserId()), command);
        } catch (RuntimeException exception) {
            log.warn("Failed to send open mat comment push notification. openMatId={}", event.openMatId(), exception);
        }
    }

    private String titleFor(PushNotificationType type) {
        return switch (type) {
            case OPEN_MAT_COMMENT_CREATED -> "오픈매트에 새 댓글이 달렸습니다";
            case OPEN_MAT_COMMENT_REPLY_CREATED -> "오픈매트 댓글에 새 답글이 달렸습니다";
            default -> "오픈매트 알림";
        };
    }

    private String bodyFor(OpenMatCommentNotificationEvent event) {
        return switch (event.type()) {
            case OPEN_MAT_COMMENT_CREATED ->
                    event.actorNickname() + "님이 \"" + event.openMatTitle() + "\" 오픈매트에 댓글을 남겼습니다.";
            case OPEN_MAT_COMMENT_REPLY_CREATED ->
                    event.actorNickname() + "님이 \"" + event.openMatTitle() + "\" 오픈매트 댓글에 답글을 남겼습니다.";
            default -> event.actorNickname() + "님이 오픈매트에 반응했습니다.";
        };
    }
}
