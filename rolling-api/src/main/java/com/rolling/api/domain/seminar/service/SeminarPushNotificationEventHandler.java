package com.rolling.api.domain.seminar.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
import com.rolling.api.domain.seminar.event.SeminarAppliedEvent;
import com.rolling.api.domain.seminar.event.SeminarApplicationCanceledByHostEvent;
import com.rolling.api.domain.seminar.event.SeminarApplicationCanceledEvent;
import com.rolling.api.domain.seminar.event.SeminarCanceledEvent;
import com.rolling.api.domain.seminar.event.SeminarDeletedEvent;
import com.rolling.api.domain.seminar.event.SeminarUpdatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SeminarPushNotificationEventHandler {

    private static final String SEMINAR_DETAIL_ROUTE = "/seminar/detail";

    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SeminarAppliedEvent event) {
        sendSafely(
                List.of(event.applicantUserId()),
                new PushNotificationCommand(
                        PushNotificationType.SEMINAR_APPLIED,
                        "세미나 신청이 완료되었습니다",
                        event.seminarTitle() + " 세미나 신청이 완료되었습니다.",
                        event.seminarId(),
                        Map.of("route", SEMINAR_DETAIL_ROUTE)
                ),
                event.seminarId(),
                PushNotificationType.SEMINAR_APPLIED
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SeminarApplicationCanceledEvent event) {
        sendSafely(
                List.of(event.applicantUserId()),
                new PushNotificationCommand(
                        PushNotificationType.SEMINAR_APPLICATION_CANCELED,
                        "세미나 신청이 취소되었습니다",
                        event.seminarTitle() + " 세미나 신청이 취소되었습니다.",
                        event.seminarId(),
                        Map.of("route", SEMINAR_DETAIL_ROUTE)
                ),
                event.seminarId(),
                PushNotificationType.SEMINAR_APPLICATION_CANCELED
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SeminarApplicationCanceledByHostEvent event) {
        sendSafely(
                List.of(event.applicantUserId()),
                new PushNotificationCommand(
                        PushNotificationType.SEMINAR_APPLICATION_CANCELED_BY_HOST,
                        "세미나 신청이 호스트에 의해 취소되었습니다",
                        event.seminarTitle() + " 세미나 신청이 호스트에 의해 취소되었습니다.",
                        event.seminarId(),
                        Map.of("route", SEMINAR_DETAIL_ROUTE)
                ),
                event.seminarId(),
                PushNotificationType.SEMINAR_APPLICATION_CANCELED_BY_HOST
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SeminarUpdatedEvent event) {
        sendSafely(
                event.participantUserIds(),
                new PushNotificationCommand(
                        PushNotificationType.SEMINAR_UPDATED,
                        "세미나 일정이 변경되었습니다",
                        event.seminarTitle() + " 세미나의 일정 또는 장소가 변경되었습니다.",
                        event.seminarId(),
                        Map.of("route", SEMINAR_DETAIL_ROUTE)
                ),
                event.seminarId(),
                PushNotificationType.SEMINAR_UPDATED
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SeminarDeletedEvent event) {
        sendSafely(
                event.participantUserIds(),
                new PushNotificationCommand(
                        PushNotificationType.SEMINAR_DELETED,
                        "세미나가 삭제되었습니다",
                        event.seminarTitle() + " 세미나가 삭제되었습니다.",
                        event.seminarId(),
                        Map.of("route", SEMINAR_DETAIL_ROUTE)
                ),
                event.seminarId(),
                PushNotificationType.SEMINAR_DELETED
        );
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(SeminarCanceledEvent event) {
        sendSafely(
                event.participantUserIds(),
                new PushNotificationCommand(
                        PushNotificationType.SEMINAR_CANCELED,
                        "세미나가 취소되었습니다",
                        event.seminarTitle() + " 세미나가 취소되었습니다.",
                        event.seminarId(),
                        Map.of("route", SEMINAR_DETAIL_ROUTE)
                ),
                event.seminarId(),
                PushNotificationType.SEMINAR_CANCELED
        );
    }

    private void sendSafely(
            Collection<Long> userIds,
            PushNotificationCommand command,
            Long seminarId,
            PushNotificationType type
    ) {
        log.info(
                "Dispatching seminar push notification. type={}, seminarId={}, pushServiceClass={}, userIds={}, route={}",
                type,
                seminarId,
                pushNotificationService.getClass().getName(),
                userIds,
                command.data().get("route")
        );

        try {
            notificationService.saveNotificationsForUsers(userIds, command);
        } catch (RuntimeException exception) {
            log.warn("Failed to persist seminar notification inbox. type={}, seminarId={}", type, seminarId, exception);
            return;
        }

        try {
            pushNotificationService.sendToUsers(userIds, command);
        } catch (RuntimeException exception) {
            log.warn("Failed to send seminar push notification. type={}, seminarId={}", type, seminarId, exception);
        }
    }
}
