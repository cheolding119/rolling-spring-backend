package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
import com.rolling.api.domain.openmat.event.OpenMatCommentNotificationEvent;
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
class OpenMatCommentNotificationEventHandlerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private PushNotificationService pushNotificationService;

    @Test
    @DisplayName("오픈매트 대댓글 알림은 inbox 저장과 push를 함께 시도한다")
    void handle_replyEvent_savesAndPushes() {
        OpenMatCommentNotificationEventHandler handler = new OpenMatCommentNotificationEventHandler(
                notificationService,
                pushNotificationService
        );
        OpenMatCommentNotificationEvent event = new OpenMatCommentNotificationEvent(
                10L,
                20L,
                30L,
                "reply-user",
                "주말 오픈매트",
                PushNotificationType.OPEN_MAT_COMMENT_REPLY_CREATED
        );

        handler.handle(event);

        ArgumentCaptor<PushNotificationCommand> captor = ArgumentCaptor.forClass(PushNotificationCommand.class);
        verify(notificationService).saveNotificationsForUsers(eq(List.of(20L)), captor.capture());
        verify(pushNotificationService).sendToUsers(eq(List.of(20L)), eq(captor.getValue()));

        PushNotificationCommand command = captor.getValue();
        assertThat(command.type()).isEqualTo(PushNotificationType.OPEN_MAT_COMMENT_REPLY_CREATED);
        assertThat(command.data()).containsEntry("route", "/openmat/detail");
        assertThat(command.title()).contains("답글");
        assertThat(command.body()).contains("reply-user");
    }

    @Test
    @DisplayName("오픈매트 댓글 알림은 댓글 타입과 상세 route를 저장한다")
    void handle_commentEvent_savesAndPushes() {
        OpenMatCommentNotificationEventHandler handler = new OpenMatCommentNotificationEventHandler(
                notificationService,
                pushNotificationService
        );
        OpenMatCommentNotificationEvent event = new OpenMatCommentNotificationEvent(
                11L,
                21L,
                31L,
                "comment-user",
                "평일 오픈매트",
                PushNotificationType.OPEN_MAT_COMMENT_CREATED
        );

        handler.handle(event);

        ArgumentCaptor<PushNotificationCommand> captor = ArgumentCaptor.forClass(PushNotificationCommand.class);
        verify(notificationService).saveNotificationsForUsers(eq(List.of(21L)), captor.capture());
        verify(pushNotificationService).sendToUsers(eq(List.of(21L)), eq(captor.getValue()));

        PushNotificationCommand command = captor.getValue();
        assertThat(command.type()).isEqualTo(PushNotificationType.OPEN_MAT_COMMENT_CREATED);
        assertThat(command.data()).containsEntry("route", "/openmat/detail");
        assertThat(command.title()).contains("댓글");
        assertThat(command.body()).contains("comment-user");
    }
}
