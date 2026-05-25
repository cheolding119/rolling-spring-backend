package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
import com.rolling.api.domain.traininglog.event.FriendRequestNotificationEvent;
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
public class FriendRequestNotificationEventHandler {

    private static final String FRIEND_REQUEST_ROUTE = "/training-log/social/friends";

    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(FriendRequestNotificationEvent event) {
        PushNotificationCommand command = new PushNotificationCommand(
                event.type(),
                titleFor(event.type()),
                bodyFor(event),
                event.requestId(),
                Map.of("route", FRIEND_REQUEST_ROUTE)
        );

        try {
            notificationService.saveNotificationsForUsers(List.of(event.recipientUserId()), command);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist friend request notification inbox. requestId={}", event.requestId(), exception);
            return;
        }

        try {
            pushNotificationService.sendToUsers(List.of(event.recipientUserId()), command);
        } catch (RuntimeException exception) {
            log.warn("Failed to send friend request push notification. requestId={}", event.requestId(), exception);
        }
    }

    private String titleFor(PushNotificationType type) {
        return switch (type) {
            case FRIEND_REQUEST_RECEIVED -> "새 친구 요청이 도착했습니다";
            default -> "친구 알림";
        };
    }

    private String bodyFor(FriendRequestNotificationEvent event) {
        return switch (event.type()) {
            case FRIEND_REQUEST_RECEIVED -> event.senderNickname() + "님이 친구 요청을 보냈습니다.";
            default -> event.senderNickname() + "님이 친구 기능에 반응했습니다.";
        };
    }
}
