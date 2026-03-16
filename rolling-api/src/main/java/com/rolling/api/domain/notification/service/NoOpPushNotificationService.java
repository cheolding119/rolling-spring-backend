package com.rolling.api.domain.notification.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Slf4j
@Service
public class NoOpPushNotificationService implements PushNotificationService {

    @Override
    public void sendToUsers(Collection<Long> userIds, PushNotificationCommand command) {
        log.info(
                "Skipping push notification because Firebase is disabled. type={}, userCount= {}",
                command.type(),
                userIds == null ? 0 : userIds.size()
        );
    }
}
