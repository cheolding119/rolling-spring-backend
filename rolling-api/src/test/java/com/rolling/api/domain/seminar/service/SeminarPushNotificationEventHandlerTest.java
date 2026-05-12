package com.rolling.api.domain.seminar.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
import com.rolling.api.domain.seminar.event.SeminarAppliedEvent;
import com.rolling.api.domain.seminar.event.SeminarCanceledEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SeminarPushNotificationEventHandlerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private SeminarPushNotificationEventHandler handler;

    @Test
    @DisplayName("세미나 신청 완료 이벤트는 알림함 저장 후 푸시 발송으로 변환된다")
    void handleAppliedEvent_savesNotificationAndSendsPush() {
        handler.handle(new SeminarAppliedEvent(11L, "가드 패스 세미나", 2L));

        ArgumentCaptor<PushNotificationCommand> commandCaptor = ArgumentCaptor.forClass(PushNotificationCommand.class);
        InOrder inOrder = inOrder(notificationService, pushNotificationService);
        inOrder.verify(notificationService).saveNotificationsForUsers(eq(List.of(2L)), commandCaptor.capture());
        inOrder.verify(pushNotificationService).sendToUsers(eq(List.of(2L)), eq(commandCaptor.getValue()));

        PushNotificationCommand command = commandCaptor.getValue();
        assertThat(command.type()).isEqualTo(PushNotificationType.SEMINAR_APPLIED);
        assertThat(command.targetId()).isEqualTo(11L);
        assertThat(command.title()).isEqualTo("세미나 신청이 완료되었습니다");
        assertThat(command.body()).isEqualTo("가드 패스 세미나 세미나 신청이 완료되었습니다.");
        assertThat(command.data()).containsEntry("route", "/seminar/detail");
    }

    @Test
    @DisplayName("세미나 취소 이벤트는 참가자들에게 취소 알림을 저장하고 푸시를 시도한다")
    void handleCanceledEvent_savesNotificationAndSendsPush() {
        handler.handle(new SeminarCanceledEvent(12L, "레그락 세미나", List.of(2L, 3L)));

        ArgumentCaptor<PushNotificationCommand> commandCaptor = ArgumentCaptor.forClass(PushNotificationCommand.class);
        InOrder inOrder = inOrder(notificationService, pushNotificationService);
        inOrder.verify(notificationService).saveNotificationsForUsers(eq(List.of(2L, 3L)), commandCaptor.capture());
        inOrder.verify(pushNotificationService).sendToUsers(eq(List.of(2L, 3L)), eq(commandCaptor.getValue()));

        PushNotificationCommand command = commandCaptor.getValue();
        assertThat(command.type()).isEqualTo(PushNotificationType.SEMINAR_CANCELED);
        assertThat(command.title()).isEqualTo("세미나가 취소되었습니다");
        assertThat(command.body()).isEqualTo("레그락 세미나 세미나가 취소되었습니다.");
    }

    @Test
    @DisplayName("알림함 저장이 실패하면 푸시는 발송하지 않는다")
    void handleAppliedEvent_whenNotificationSaveFails_doesNotSendPush() {
        doThrow(new IllegalStateException("save failed"))
                .when(notificationService)
                .saveNotificationsForUsers(eq(List.of(2L)), any(PushNotificationCommand.class));

        handler.handle(new SeminarAppliedEvent(13L, "패싱 세미나", 2L));

        verify(pushNotificationService, never()).sendToUsers(eq(List.of(2L)), any(PushNotificationCommand.class));
    }
}
