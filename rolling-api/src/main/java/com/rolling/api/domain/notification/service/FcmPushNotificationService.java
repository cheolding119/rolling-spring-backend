package com.rolling.api.domain.notification.service;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.user.entity.UserDevice;
import com.rolling.api.domain.user.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnBean(FirebaseMessaging.class)
public class FcmPushNotificationService implements PushNotificationService {

    private static final int MAX_MULTICAST_SIZE = 500;

    private final FirebaseMessaging firebaseMessaging;
    private final UserDeviceRepository userDeviceRepository;

    @Override
    public void sendToUsers(Collection<Long> userIds, PushNotificationCommand command) {
        List<Long> normalizedUserIds = normalizeUserIds(userIds);
        if (normalizedUserIds.isEmpty()) {
            return;
        }

        List<String> tokens = userDeviceRepository.findAllByUser_IdIn(normalizedUserIds).stream()
                .map(UserDevice::getFcmToken)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        if (tokens.isEmpty()) {
            log.debug("Skipping push notification because no device tokens were found. type={}", command.type());
            return;
        }

        List<String> invalidTokens = new ArrayList<>();
        Map<String, String> data = buildData(command);

        for (int i = 0; i < tokens.size(); i += MAX_MULTICAST_SIZE) {
            List<String> batch = tokens.subList(i, Math.min(i + MAX_MULTICAST_SIZE, tokens.size()));

            try {
                BatchResponse response = firebaseMessaging.sendEachForMulticast(
                        MulticastMessage.builder()
                                .addAllTokens(batch)
                                .setNotification(Notification.builder()
                                        .setTitle(command.title())
                                        .setBody(command.body())
                                        .build())
                                .putAllData(data)
                                .build()
                );

                collectInvalidTokens(batch, response.getResponses(), invalidTokens);
                log.info(
                        "Sent push notification. type={}, tokenCount={}, successCount={}, failureCount={}",
                        command.type(),
                        batch.size(),
                        response.getSuccessCount(),
                        response.getFailureCount()
                );
            } catch (FirebaseMessagingException exception) {
                throw new IllegalStateException("FCM push send failed", exception);
            }
        }

        if (!invalidTokens.isEmpty()) {
            userDeviceRepository.deleteAllByFcmTokenIn(new LinkedHashSet<>(invalidTokens));
            log.info("Deleted {} invalid FCM tokens", invalidTokens.size());
        }
    }

    private List<Long> normalizeUserIds(Collection<Long> userIds) {
        if (userIds == null) {
            return List.of();
        }

        return userIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }

    private Map<String, String> buildData(PushNotificationCommand command) {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("type", command.type().name());
        if (command.targetId() != null) {
            data.put("targetId", String.valueOf(command.targetId()));
        }
        data.putAll(command.data());
        return data;
    }

    private void collectInvalidTokens(List<String> tokens, List<SendResponse> responses, List<String> invalidTokens) {
        for (int i = 0; i < Math.min(tokens.size(), responses.size()); i++) {
            SendResponse response = responses.get(i);
            if (response.isSuccessful()) {
                continue;
            }

            FirebaseMessagingException exception = response.getException();
            if (exception == null || exception.getMessagingErrorCode() == null) {
                continue;
            }

            MessagingErrorCode errorCode = exception.getMessagingErrorCode();
            if (errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT) {
                invalidTokens.add(tokens.get(i));
            }
        }
    }
}
