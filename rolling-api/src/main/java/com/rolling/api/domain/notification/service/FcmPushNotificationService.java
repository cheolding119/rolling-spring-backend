package com.rolling.api.domain.notification.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.Aps;
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
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
public class FcmPushNotificationService implements PushNotificationService {

    private static final int MAX_MULTICAST_SIZE = 500;
    private static final int TOKEN_PREFIX_LENGTH = 12;
    private static final String DEFAULT_SOUND = "default";
    private static final String RETRY_POLICY = "NO_RETRY_AUTOMATIC";

    private final FirebaseMessaging firebaseMessaging;
    private final UserDeviceRepository userDeviceRepository;
    private final String androidChannelId;

    @Override
    public void sendToUsers(Collection<Long> userIds, PushNotificationCommand command) {
        List<Long> normalizedUserIds = normalizeUserIds(userIds);
        if (normalizedUserIds.isEmpty()) {
            log.info("Skipping push notification because target userIds are empty. type={}", command.type());
            return;
        }

        List<UserDevice> userDevices = userDeviceRepository.findPushTargetDevicesByUserIds(normalizedUserIds);
        List<String> tokens = userDevices.stream()
                .map(UserDevice::getFcmToken)
                .filter(StringUtils::hasText)
                .distinct()
                .toList();

        log.info(
                "Resolved FCM target devices. type={}, targetUserIds={}, deviceCount={}, tokenPrefixes={}",
                command.type(),
                normalizedUserIds,
                userDevices.size(),
                summarizeTokens(tokens)
        );

        if (tokens.isEmpty()) {
            log.warn(
                    "Skipping push notification because no device tokens were found. type={}, targetUserIds={}",
                    command.type(),
                    normalizedUserIds
            );
            return;
        }

        List<String> invalidTokens = new ArrayList<>();
        Map<String, String> data = buildData(command);

        for (int i = 0; i < tokens.size(); i += MAX_MULTICAST_SIZE) {
            List<String> batch = tokens.subList(i, Math.min(i + MAX_MULTICAST_SIZE, tokens.size()));

            try {
                BatchResponse response = firebaseMessaging.sendEachForMulticast(buildMessage(batch, command, data));

                collectInvalidTokens(command, batch, response.getResponses(), invalidTokens);
                log.info(
                        "Sent push notification. type={}, tokenCount={}, successCount={}, failureCount={}, tokenPrefixes={}, retryPolicy={}",
                        command.type(),
                        batch.size(),
                        response.getSuccessCount(),
                        response.getFailureCount(),
                        summarizeTokens(batch),
                        RETRY_POLICY
                );
            } catch (FirebaseMessagingException exception) {
                log.error(
                        "FCM batch send failed. type={}, tokenCount={}, tokenPrefixes={}, errorCode={}, retryPolicy={}",
                        command.type(),
                        batch.size(),
                        summarizeTokens(batch),
                        exception.getMessagingErrorCode(),
                        RETRY_POLICY,
                        exception
                );
                throw new IllegalStateException("FCM push send failed", exception);
            }
        }

        if (!invalidTokens.isEmpty()) {
            userDeviceRepository.deleteAllByFcmTokenIn(new LinkedHashSet<>(invalidTokens));
            log.info("Deleted {} invalid FCM tokens. tokenPrefixes={}", invalidTokens.size(), summarizeTokens(invalidTokens));
        }
    }

    private MulticastMessage buildMessage(List<String> tokens, PushNotificationCommand command, Map<String, String> data) {
        return MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(command.title())
                        .setBody(command.body())
                        .build())
                .setAndroidConfig(buildAndroidConfig(command))
                .setApnsConfig(buildApnsConfig())
                .putAllData(data)
                .build();
    }

    private AndroidConfig buildAndroidConfig(PushNotificationCommand command) {
        return AndroidConfig.builder()
                .setPriority(AndroidConfig.Priority.HIGH)
                .setNotification(AndroidNotification.builder()
                        .setTitle(command.title())
                        .setBody(command.body())
                        .setChannelId(androidChannelId)
                        .setPriority(AndroidNotification.Priority.HIGH)
                        .setDefaultSound(true)
                        .setDefaultVibrateTimings(true)
                        .build())
                .build();
    }

    private ApnsConfig buildApnsConfig() {
        return ApnsConfig.builder()
                .putHeader("apns-push-type", "alert")
                .putHeader("apns-priority", "10")
                .setAps(Aps.builder()
                        .setSound(DEFAULT_SOUND)
                        .build())
                .build();
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
        data.putAll(command.data());
        data.put("type", command.type().name());
        if (command.targetId() != null) {
            data.put("targetId", String.valueOf(command.targetId()));
        }
        if (StringUtils.hasText(command.title())) {
            data.put("title", command.title());
        }
        if (StringUtils.hasText(command.body())) {
            data.put("body", command.body());
        }
        return data;
    }

    private void collectInvalidTokens(
            PushNotificationCommand command,
            List<String> tokens,
            List<SendResponse> responses,
            List<String> invalidTokens
    ) {
        for (int i = 0; i < Math.min(tokens.size(), responses.size()); i++) {
            SendResponse response = responses.get(i);
            if (response.isSuccessful()) {
                continue;
            }

            FirebaseMessagingException exception = response.getException();
            if (exception == null || exception.getMessagingErrorCode() == null) {
                log.warn(
                        "FCM token send failed without error code. type={}, tokenPrefix={}, retryPolicy={}",
                        command.type(),
                        summarizeToken(tokens.get(i)),
                        RETRY_POLICY
                );
                continue;
            }

            MessagingErrorCode errorCode = exception.getMessagingErrorCode();
            boolean cleanupTarget = isInvalidTokenError(errorCode);

            log.warn(
                    "FCM token send failed. type={}, tokenPrefix={}, errorCode={}, cleanupTarget={}, retryPolicy={}",
                    command.type(),
                    summarizeToken(tokens.get(i)),
                    errorCode,
                    cleanupTarget,
                    RETRY_POLICY
            );

            if (cleanupTarget) {
                invalidTokens.add(tokens.get(i));
            }
        }
    }

    private boolean isInvalidTokenError(MessagingErrorCode errorCode) {
        return errorCode == MessagingErrorCode.UNREGISTERED || errorCode == MessagingErrorCode.INVALID_ARGUMENT;
    }

    private List<String> summarizeTokens(Collection<String> tokens) {
        return tokens.stream()
                .map(this::summarizeToken)
                .toList();
    }

    private String summarizeToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "<blank>";
        }

        int prefixLength = Math.min(TOKEN_PREFIX_LENGTH, token.length());
        String prefix = token.substring(0, prefixLength);
        return token.length() > prefixLength ? prefix + "..." : prefix;
    }
}

