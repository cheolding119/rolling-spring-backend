package com.rolling.api.domain.notification.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FcmPushNotificationServiceTest {

    @Mock
    private FirebaseMessaging firebaseMessaging;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @InjectMocks
    private FcmPushNotificationService fcmPushNotificationService;

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

        when(userDeviceRepository.findAllByUser_IdIn(List.of(1L, 2L)))
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
                        Map.of("openMatId", "99")
                )
        );

        ArgumentCaptor<MulticastMessage> messageCaptor = ArgumentCaptor.forClass(MulticastMessage.class);
        verify(firebaseMessaging).sendEachForMulticast(messageCaptor.capture());

        MulticastMessage message = messageCaptor.getValue();
        @SuppressWarnings("unchecked")
        List<String> tokens = (List<String>) ReflectionTestUtils.getField(message, "tokens");
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) ReflectionTestUtils.getField(message, "data");

        assertThat(tokens).containsExactly("token-1", "token-2");
        assertThat(data)
                .containsEntry("type", "OPEN_MAT_UPDATED")
                .containsEntry("targetId", "99")
                .containsEntry("openMatId", "99");

        verify(userDeviceRepository).deleteAllByFcmTokenIn(java.util.Set.of("token-2"));
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
