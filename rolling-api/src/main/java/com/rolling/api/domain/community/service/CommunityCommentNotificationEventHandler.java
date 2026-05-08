package com.rolling.api.domain.community.service;

import com.rolling.api.domain.community.event.CommunityCommentCreatedEvent;
import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.notification.service.NotificationService;
import com.rolling.api.domain.notification.service.PushNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class CommunityCommentNotificationEventHandler {

    private static final String COMMUNITY_POST_DETAIL_ROUTE = "/community/posts";

    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(CommunityCommentCreatedEvent event) {
        String title = "커뮤니티에 새 댓글이 달렸습니다";
        String body = event.commenterNickname() + "님이 \"" + event.postTitle() + "\" 글에 댓글을 남겼습니다.";
        PushNotificationCommand command = new PushNotificationCommand(
                PushNotificationType.COMMUNITY_COMMENT_CREATED,
                title,
                body,
                event.postId(),
                Map.of("route", COMMUNITY_POST_DETAIL_ROUTE + "/" + event.postId())
        );

        try {
            notificationService.saveNotificationsForUsers(
                    java.util.List.of(event.postAuthorId()),
                    command
            );
        } catch (RuntimeException exception) {
            log.warn("Failed to persist community comment notification inbox. postId={}", event.postId(), exception);
            return;
        }

        try {
            pushNotificationService.sendToUsers(java.util.List.of(event.postAuthorId()), command);
        } catch (RuntimeException exception) {
            log.warn("Failed to send community comment push notification. postId={}", event.postId(), exception);
        }
    }
}
