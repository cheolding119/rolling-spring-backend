package com.rolling.api.domain.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationBadgeResponse {

    private long unreadCount;
}
