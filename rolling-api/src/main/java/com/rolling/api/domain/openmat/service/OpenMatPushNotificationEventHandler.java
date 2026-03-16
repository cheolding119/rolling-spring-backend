package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
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

    private final PushNotificationService pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OpenMatUpdatedEvent event) {
        sendSafely(
                event.participantUserIds(),
                new PushNotificationCommand(
                        PushNotificationType.OPEN_MAT_UPDATED,
                        "오픈매트 일정이 변경되었습니다",
                        event.openMatTitle() + " 오픈매트의 일정 또는 장소가 변경되었습니다.",
                        event.openMatId(),
                        Map.of("openMatId", String.valueOf(event.openMatId()))
                ),
                event.openMatId(),
                PushNotificationType.OPEN_MAT_UPDATED
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(OpenMatDeletedEvent event) {
        sendSafely(
                event.participantUserIds(),
                new PushNotificationCommand(
                        PushNotificationType.OPEN_MAT_DELETED,
                        "오픈매트가 취소되었습니다",
                        event.openMatTitle() + " 오픈매트가 삭제되었습니다.",
                        event.openMatId(),
                        Map.of("openMatId", String.valueOf(event.openMatId()))
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
        try {
            pushNotificationService.sendToUsers(userIds, command);
        } catch (RuntimeException exception) {
            log.warn("Failed to send push notification. type={}, openMatId={}", type, openMatId, exception);
        }
    }
}
