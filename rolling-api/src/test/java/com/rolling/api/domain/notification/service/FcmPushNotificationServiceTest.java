package com.rolling.api.domain.notification.service;

import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.ApnsConfig;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.rolling.api.domain.notification.model.PushNotificationCommand;
import com.rolling.api.domain.notification.model.PushNotificationType;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.entity.UserDevice;
import com.rolling.api.domain.user.repository.UserDeviceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmPushNotificationServiceTest {

    private static final String ANDROID_CHANNEL_ID = "rolling_open_mat_alerts";

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    private FcmPushNotificationService fcmPushNotificationService;

    @BeforeEach
    void setUp() {
        fcmPushNotificationService = new FcmPushNotificationService(
                firebaseMessaging,
                userDeviceRepository,
                ANDROID_CHANNEL_ID
        );
    }

    @Test
    @DisplayName("사용자 디바이스 토큰으로 푸시를 발송하고 무효 토큰은 정리한다")
    void sendToUsers_sendsPushAndDeletesInvalidTokens() throws Exception {
        User firstUser = createUser(1L, "user-1");
        User secondUser = createUser(2L, "user-2");

        UserDevice firstDevice = UserDevice.builder()
                .user(firstUser)
                .fcmToken("token-1")
                .build();
        UserDevice duplicateDevice = UserDevice.builder()
                .user(firstUser)
                .fcmToken("token-1")
                .build();
        UserDevice secondDevice = UserDevice.builder()
                .user(secondUser)
                .fcmToken("token-2")
                .build();

        when(userDeviceRepository.findPushTargetDevicesByUserIds(List.of(1L, 2L)))
                .thenReturn(List.of(firstDevice, duplicateDevice, secondDevice));

        BatchResponse batchResponse = mock(BatchResponse.class);
        SendResponse successResponse = mock(SendResponse.class);
        SendResponse failedResponse = mock(SendResponse.class);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);

        when(successResponse.isSuccessful()).thenReturn(true);
        when(failedResponse.isSuccessful()).thenReturn(false);
        when(failedResponse.getException()).thenReturn(exception);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.UNREGISTERED);
        when(batchResponse.getResponses()).thenReturn(List.of(successResponse, failedResponse));
        when(batchResponse.getSuccessCount()).thenReturn(1);
        when(batchResponse.getFailureCount()).thenReturn(1);
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenReturn(batchResponse);

        fcmPushNotificationService.sendToUsers(
                List.of(1L, 2L),
                new PushNotificationCommand(
                        PushNotificationType.OPEN_MAT_UPDATED,
                        "오픈매트 일정이 변경되었습니다",
                        "테스트 알림입니다.",
                        99L,
                        Map.of("route", "/openmat/detail")
                )
        );

        ArgumentCaptor<MulticastMessage> messageCaptor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebaseMessaging).sendEachForMulticast(messageCaptor.capture());

        MulticastMessage message = messageCaptor.getValue();
        @SuppressWarnings("unchecked")
        List<String> tokens = (List<String>) ReflectionTestUtils.getField(message, "tokens");
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) ReflectionTestUtils.getField(message, "data");
        AndroidConfig androidConfig = (AndroidConfig) ReflectionTestUtils.getField(message, "androidConfig");
        AndroidNotification androidNotification =
                (AndroidNotification) ReflectionTestUtils.getField(androidConfig, "notification");
        ApnsConfig apnsConfig = (ApnsConfig) ReflectionTestUtils.getField(message, "apnsConfig");
        @SuppressWarnings("unchecked")
        Map<String, String> apnsHeaders = (Map<String, String>) ReflectionTestUtils.getField(apnsConfig, "headers");
        @SuppressWarnings("unchecked")
        Map<String, Object> apnsPayload = (Map<String, Object>) ReflectionTestUtils.getField(apnsConfig, "payload");
        @SuppressWarnings("unchecked")
        Map<String, Object> aps = (Map<String, Object>) apnsPayload.get("aps");

        assertThat(tokens).containsExactly("token-1", "token-2");
        assertThat(data)
                .containsEntry("type", "OPEN_MAT_UPDATED")
                .containsEntry("targetId", "99")
                .containsEntry("route", "/openmat/detail")
                .containsEntry("title", "오픈매트 일정이 변경되었습니다")
                .containsEntry("body", "테스트 알림입니다.")
                .doesNotContainKey("openMatId");
        assertThat(ReflectionTestUtils.getField(androidConfig, "priority")).isEqualTo("high");
        assertThat(ReflectionTestUtils.getField(androidNotification, "channelId")).isEqualTo(ANDROID_CHANNEL_ID);
        assertThat(ReflectionTestUtils.getField(androidNotification, "priority")).isEqualTo("PRIORITY_HIGH");
        assertThat(ReflectionTestUtils.getField(androidNotification, "defaultSound")).isEqualTo(true);
        assertThat(ReflectionTestUtils.getField(androidNotification, "defaultVibrateTimings")).isEqualTo(true);
        assertThat(apnsHeaders)
                .containsEntry("apns-push-type", "alert")
                .containsEntry("apns-priority", "10");
        assertThat(aps).containsEntry("sound", "default");

        verify(userDeviceRepository).deleteAllByFcmTokenIn(java.util.Set.of("token-2"));
    }

    @Test
    @DisplayName("FCM 배치 전송 예외가 발생하면 자동 재시도 없이 예외를 그대로 올린다")
    void sendToUsers_whenBatchSendFails_throwsWithoutRetry() throws Exception {
        User firstUser = createUser(1L, "user-1");
        UserDevice firstDevice = UserDevice.builder()
                .user(firstUser)
                .fcmToken("token-1")
                .build();

        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);

        when(userDeviceRepository.findPushTargetDevicesByUserIds(List.of(1L)))
                .thenReturn(List.of(firstDevice));
        when(firebaseMessaging.sendEachForMulticast(any(MulticastMessage.class))).thenThrow(exception);
        when(exception.getMessagingErrorCode()).thenReturn(MessagingErrorCode.INTERNAL);

        assertThatThrownBy(() -> fcmPushNotificationService.sendToUsers(
                List.of(1L),
                new PushNotificationCommand(
                        PushNotificationType.OPEN_MAT_UPDATED,
                        "오픈매트 일정이 변경되었습니다",
                        "테스트 알림입니다.",
                        99L,
                        Map.of("route", "/openmat/detail")
                )
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("FCM push send failed");

        verify(firebaseMessaging).sendEachForMulticast(any(MulticastMessage.class));
        verify(userDeviceRepository, never()).deleteAllByFcmTokenIn(any());
    }

    private User createUser(Long id, String socialId) {
        User user = User.builder()
                .socialId(socialId)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname(socialId)
                .email(socialId + "@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}

