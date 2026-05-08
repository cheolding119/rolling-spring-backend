package com.rolling.api.domain.community.service;

import com.rolling.api.domain.community.event.CommunityCommentCreatedEvent;
import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
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
class CommunityCommentNotificationEventHandlerTest {

    @Mock
    private NotificationService notificationService;

    @Mock
    private PushNotificationService pushNotificationService;

    @Test
    @DisplayName("댓글 생성 이벤트는 게시글 작성자에게 커뮤니티 알림을 저장하고 푸시한다")
    void handle_savesInboxAndPushesNotification() {
        CommunityCommentNotificationEventHandler handler = new CommunityCommentNotificationEventHandler(
                notificationService,
                pushNotificationService
        );
        CommunityCommentCreatedEvent event = new CommunityCommentCreatedEvent(
                100L,
                10L,
                20L,
                30L,
                "commenter",
                "암바 방어 질문",
                "좋은 글입니다"
        );

        handler.handle(event);

        ArgumentCaptor<PushNotificationCommand> captor = ArgumentCaptor.forClass(PushNotificationCommand.class);
        verify(notificationService).saveNotificationsForUsers(eq(List.of(20L)), captor.capture());
        verify(pushNotificationService).sendToUsers(eq(List.of(20L)), eq(captor.getValue()));

        PushNotificationCommand command = captor.getValue();
        assertThat(command.type()).isEqualTo(PushNotificationType.COMMUNITY_COMMENT_CREATED);
        assertThat(command.data()).containsEntry("route", "/community/posts/10");
        assertThat(command.title()).contains("새 댓글");
        assertThat(command.body()).contains("commenter");
    }
}
