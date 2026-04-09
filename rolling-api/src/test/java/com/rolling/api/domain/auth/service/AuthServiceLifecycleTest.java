package com.rolling.api.domain.auth.service;

import com.rolling.api.domain.auth.dto.AuthResponse;
import com.rolling.api.domain.auth.dto.SocialLoginRequest;
import com.rolling.api.domain.auth.dto.TokenRefreshRequest;
import com.rolling.api.domain.auth.dto.TokenRefreshResponse;
import com.rolling.api.domain.auth.entity.RefreshToken;
import com.rolling.api.domain.auth.repository.RefreshTokenRepository;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.entity.UserDevice;
import com.rolling.api.domain.user.repository.UserDeviceRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.alert.OperationalAlertPublisher;
import com.rolling.api.global.monitoring.ScheduledTaskTracker;
import com.rolling.api.global.security.AdminAccessConfig;
import com.rolling.api.global.security.jwt.JwtTokenProvider;
import com.rolling.api.infra.google.GoogleClient;
import com.rolling.api.infra.google.dto.GoogleUserResponse;
import com.rolling.api.infra.kakao.KakaoClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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

    @Mock
    private AdminAccessConfig adminAccessConfig;

    @Mock
    private ScheduledTaskTracker scheduledTaskTracker;

    @Mock
    private OperationalAlertPublisher operationalAlertPublisher;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("소셜 로그인 응답에 관리자 여부를 포함한다")
    void login_includesIsAdmin() {
        SocialLoginRequest request = new SocialLoginRequest();
        ReflectionTestUtils.setField(request, "provider", "GOOGLE");
        ReflectionTestUtils.setField(request, "accessToken", "google-token");

        GoogleUserResponse googleUserResponse = new GoogleUserResponse();
        ReflectionTestUtils.setField(googleUserResponse, "sub", "social-1");
        ReflectionTestUtils.setField(googleUserResponse, "name", "관리자");
        ReflectionTestUtils.setField(googleUserResponse, "email", "admin@example.com");

        User user = User.builder()
                .socialId("social-1")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("관리자")
                .email("admin@example.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        ReflectionTestUtils.setField(authService, "accessTokenExpiry", 1800000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiry", 1209600000L);

        when(googleClient.getUserInfo("google-token")).thenReturn(googleUserResponse);
        when(userRepository.findBySocialIdAndSocialProviderAndIsWithdrawnFalse("social-1", SocialProvider.GOOGLE))
                .thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(1L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(1L)).thenReturn("refresh-token");
        when(adminAccessConfig.isAdmin(1L)).thenReturn(true);

        AuthResponse response = authService.login(request);

        assertThat(response.getIsAdmin()).isTrue();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("관리자");
        verify(refreshTokenRepository).deleteByUserId(1L);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("토큰 갱신 응답에 관리자 여부를 포함한다")
    void refresh_includesIsAdmin() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiry", 1800000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiry", 1209600000L);

        TokenRefreshRequest request = new TokenRefreshRequest();
        ReflectionTestUtils.setField(request, "refreshToken", "old-refresh-token");

        RefreshToken savedToken = RefreshToken.builder()
                .token("old-refresh-token")
                .userId(2L)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByToken("old-refresh-token")).thenReturn(Optional.of(savedToken));
        when(jwtTokenProvider.validateToken("old-refresh-token")).thenReturn(true);
        when(jwtTokenProvider.createAccessToken(2L)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(2L)).thenReturn("new-refresh-token");
        when(adminAccessConfig.isAdmin(2L)).thenReturn(false);

        TokenRefreshResponse response = authService.refresh(request);

        assertThat(response.getIsAdmin()).isFalse();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        verify(refreshTokenRepository).delete(savedToken);
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

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

    @Test
    @DisplayName("로그아웃 시 전달한 FCM 토큰이 없어도 리프레시 토큰 무효화는 계속 진행한다")
    void logout_withMissingFcmTokenStillDeletesRefreshToken() {
        when(userDeviceRepository.findByUser_IdAndFcmToken(52L, "missing-token")).thenReturn(Optional.empty());

        authService.logout(52L, " missing-token ");

        verify(userDeviceRepository).findByUser_IdAndFcmToken(52L, "missing-token");
        verify(userDeviceRepository, never()).delete(org.mockito.ArgumentMatchers.any(UserDevice.class));
        verify(refreshTokenRepository).deleteByUserId(52L);
    }
}
