package com.rolling.api.domain.notification.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
public class NoOpPushNotificationService implements PushNotificationService {

    private final String reason;

    public NoOpPushNotificationService() {
        this("no reason provided");
    }

    @Override
    public void sendToUsers(Collection<Long> userIds, PushNotificationCommand command) {
        log.warn(
                "Skipping push notification because NoOpPushNotificationService is active. type={}, userCount={}, reason={}",
                command.type(),
                userIds == null ? 0 : userIds.size(),
                reason
        );
    }
}
