package com.rolling.api.domain.auth.service;

import com.rolling.api.domain.auth.repository.RefreshTokenRepository;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.entity.UserDevice;
import com.rolling.api.domain.user.repository.UserDeviceRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.security.jwt.JwtTokenProvider;
import com.rolling.api.infra.google.GoogleClient;
import com.rolling.api.infra.kakao.KakaoClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLifecycleTest {

    @Mock
    private KakaoClient kakaoClient;

    @Mock
    private GoogleClient googleClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("로그아웃 시 현재 디바이스 FCM 토큰과 리프레시 토큰을 함께 정리한다")
    void logout_removesCurrentDeviceTokenAndRefreshToken() {
        User user = User.builder()
                .socialId("social-logout")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("logout-user")
                .email("logout@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 50L);

        UserDevice userDevice = UserDevice.builder()
                .user(user)
                .fcmToken("logout-token")
                .platform("ANDROID")
                .deviceId("device-logout")
                .appVersion("1.0.0")
                .build();

        when(userDeviceRepository.findByUser_IdAndFcmToken(50L, "logout-token")).thenReturn(Optional.of(userDevice));

        authService.logout(50L, "logout-token");

        assertThat(user.getDevices()).isEmpty();
        verify(userDeviceRepository).delete(userDevice);
        verify(refreshTokenRepository).deleteByUserId(50L);
    }

    @Test
    @DisplayName("로그아웃 요청에 FCM 토큰이 없으면 리프레시 토큰만 무효화한다")
    void logout_withoutFcmTokenOnlyDeletesRefreshToken() {
        authService.logout(51L, null);

        verify(refreshTokenRepository).deleteByUserId(51L);
        verify(userDeviceRepository, never()).findByUser_IdAndFcmToken(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString());
        verify(userDeviceRepository, never()).delete(org.mockito.ArgumentMatchers.any(UserDevice.class));
    }
}
