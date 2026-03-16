package com.rolling.api.domain.notification.service;

import com.rolling.api.domain.notification.model.PushNotificationCommand;

import java.util.Collection;

public interface PushNotificationService {

    void sendToUsers(Collection<Long> userIds, PushNotificationCommand command);
}
