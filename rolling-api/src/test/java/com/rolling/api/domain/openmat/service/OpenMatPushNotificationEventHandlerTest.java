package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
import com.rolling.api.domain.openmat.event.OpenMatDeletedEvent;
import com.rolling.api.domain.openmat.event.OpenMatUpdatedEvent;
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
class OpenMatPushNotificationEventHandlerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private PushNotificationService pushNotificationService;

    @InjectMocks
    private OpenMatPushNotificationEventHandler handler;

    @Test
    @DisplayName("오픈매트 수정 이벤트는 OPEN_MAT_UPDATED 푸시와 알림함 저장으로 변환된다")
    void handleUpdatedEvent_savesNotificationAndSendsUpdatedPush() {
        handler.handle(new OpenMatUpdatedEvent(11L, "주말 오픈매트", List.of(2L, 3L)));

        ArgumentCaptor<PushNotificationCommand> commandCaptor = ArgumentCaptor.forClass(PushNotificationCommand.class);
        InOrder inOrder = inOrder(notificationService, pushNotificationService);
        inOrder.verify(notificationService).saveNotificationsForUsers(eq(List.of(2L, 3L)), commandCaptor.capture());
        inOrder.verify(pushNotificationService).sendToUsers(eq(List.of(2L, 3L)), eq(commandCaptor.getValue()));

        PushNotificationCommand command = commandCaptor.getValue();
        assertThat(command.type()).isEqualTo(PushNotificationType.OPEN_MAT_UPDATED);
        assertThat(command.targetId()).isEqualTo(11L);
        assertThat(command.title()).isEqualTo("오픈매트 일정이 변경되었습니다");
        assertThat(command.body()).isEqualTo("주말 오픈매트 오픈매트의 일정 또는 장소가 변경되었습니다.");
        assertThat(command.data())
                .containsEntry("route", "/openmat/detail")
                .doesNotContainKey("openMatId");
    }

    @Test
    @DisplayName("오픈매트 삭제 이벤트는 OPEN_MAT_DELETED 푸시와 알림함 저장으로 변환된다")
    void handleDeletedEvent_savesNotificationAndSendsDeletedPush() {
        handler.handle(new OpenMatDeletedEvent(12L, "평일 오픈매트", List.of(5L)));

        ArgumentCaptor<PushNotificationCommand> commandCaptor = ArgumentCaptor.forClass(PushNotificationCommand.class);
        InOrder inOrder = inOrder(notificationService, pushNotificationService);
        inOrder.verify(notificationService).saveNotificationsForUsers(eq(List.of(5L)), commandCaptor.capture());
        inOrder.verify(pushNotificationService).sendToUsers(eq(List.of(5L)), eq(commandCaptor.getValue()));

        PushNotificationCommand command = commandCaptor.getValue();
        assertThat(command.type()).isEqualTo(PushNotificationType.OPEN_MAT_DELETED);
        assertThat(command.targetId()).isEqualTo(12L);
        assertThat(command.title()).isEqualTo("오픈매트가 취소되었습니다");
        assertThat(command.body()).isEqualTo("평일 오픈매트 오픈매트가 삭제되었습니다.");
        assertThat(command.data())
                .containsEntry("route", "/openmat")
                .doesNotContainKey("openMatId");
    }

    @Test
    @DisplayName("알림함 저장이 실패하면 푸시는 발송하지 않는다")
    void handleUpdatedEvent_whenNotificationSaveFails_doesNotSendPush() {
        doThrow(new IllegalStateException("save failed"))
                .when(notificationService)
                .saveNotificationsForUsers(eq(List.of(2L)), any(PushNotificationCommand.class));

        handler.handle(new OpenMatUpdatedEvent(13L, "야간 오픈매트", List.of(2L)));

        verify(pushNotificationService, never()).sendToUsers(eq(List.of(2L)), any(PushNotificationCommand.class));
    }
}
