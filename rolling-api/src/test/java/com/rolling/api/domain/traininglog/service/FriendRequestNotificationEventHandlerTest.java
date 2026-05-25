package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
import com.rolling.api.domain.traininglog.event.FriendRequestNotificationEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FriendRequestNotificationEventHandlerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private PushNotificationService pushNotificationService;

    @Test
    @DisplayName("친구 요청 알림은 inbox 저장과 push를 함께 시도한다")
    void handle_requestEvent_savesAndPushes() {
        FriendRequestNotificationEventHandler handler = new FriendRequestNotificationEventHandler(
                notificationService,
                pushNotificationService
        );
        FriendRequestNotificationEvent event = new FriendRequestNotificationEvent(
                88L,
                10L,
                "민준",
                20L,
                PushNotificationType.FRIEND_REQUEST_RECEIVED
        );

        handler.handle(event);

        ArgumentCaptor<PushNotificationCommand> captor = ArgumentCaptor.forClass(PushNotificationCommand.class);
        verify(notificationService).saveNotificationsForUsers(eq(List.of(20L)), captor.capture());
        verify(pushNotificationService).sendToUsers(eq(List.of(20L)), eq(captor.getValue()));

        PushNotificationCommand command = captor.getValue();
        assertThat(command.type()).isEqualTo(PushNotificationType.FRIEND_REQUEST_RECEIVED);
        assertThat(command.targetId()).isEqualTo(88L);
        assertThat(command.data()).containsEntry("route", "/training-log/social/friends");
        assertThat(command.title()).contains("친구 요청");
        assertThat(command.body()).contains("민준");
    }
}
