package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
import com.rolling.api.domain.openmat.event.OpenMatDeletedEvent;
import com.rolling.api.domain.openmat.event.OpenMatUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenMatPushNotificationEventHandler {

    private static final String OPEN_MAT_DETAIL_ROUTE = "/openmat/detail";

    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OpenMatUpdatedEvent event) {
        String title = "오픈매트 일정이 변경되었습니다";
        String body = event.openMatTitle() + " 오픈매트의 일정 또는 장소가 변경되었습니다.";
        sendSafely(
                event.participantUserIds(),
                new PushNotificationCommand(
                        PushNotificationType.OPEN_MAT_UPDATED,
                        title,
                        body,
                        event.openMatId(),
                        Map.of("route", OPEN_MAT_DETAIL_ROUTE)
                ),
                event.openMatId(),
                PushNotificationType.OPEN_MAT_UPDATED
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OpenMatDeletedEvent event) {
        String title = "오픈매트가 취소되었습니다";
        String body = event.openMatTitle() + " 오픈매트가 삭제되었습니다.";
        sendSafely(
                event.participantUserIds(),
                new PushNotificationCommand(
                        PushNotificationType.OPEN_MAT_DELETED,
                        title,
                        body,
                        event.openMatId(),
                        Map.of("route", OPEN_MAT_DETAIL_ROUTE)
                ),
                event.openMatId(),
                PushNotificationType.OPEN_MAT_DELETED
        );
    }

    private void sendSafely(
            java.util.Collection<Long> userIds,
            PushNotificationCommand command,
            Long openMatId,
            PushNotificationType type
    ) {
        log.info(
                "Dispatching push notification. type={}, openMatId={}, pushServiceClass={}, userIds={}, route={}",
                type,
                openMatId,
                pushNotificationService.getClass().getName(),
                userIds,
                command.data().get("route")
        );

        try {
            notificationService.saveNotificationsForUsers(userIds, command);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist notification inbox. type={}, openMatId={}", type, openMatId, exception);
            return;
        }

        try {
            pushNotificationService.sendToUsers(userIds, command);
        } catch (RuntimeException exception) {
            log.warn("Failed to send push notification. type={}, openMatId={}", type, openMatId, exception);
        }
    }
}


