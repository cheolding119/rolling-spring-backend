package com.rolling.api.domain.notification.dto;

import com.rolling.api.domain.notification.entity.Notification;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {

    private Long id;
    private String type;
    private Long targetId;
    private String route;
    private String title;
    private String body;
    private LocalDateTime readAt;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .type(notification.getType().name())
                .targetId(notification.getTargetId())
                .route(notification.getRoute())
                .title(notification.getTitle())
                .body(notification.getBody())
                .readAt(notification.getReadAt())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
