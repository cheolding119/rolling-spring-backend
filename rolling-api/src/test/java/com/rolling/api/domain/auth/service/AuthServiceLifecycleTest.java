package com.rolling.api.domain.auth.service;

import com.rolling.api.domain.auth.dto.AuthResponse;
import com.rolling.api.domain.auth.dto.SocialLoginRequest;
import com.rolling.api.domain.auth.dto.TokenRefreshRequest;
import com.rolling.api.domain.auth.dto.TokenRefreshResponse;
import com.rolling.api.domain.auth.entity.RefreshToken;
import com.rolling.api.domain.auth.repository.RefreshTokenRepository;
import com.rolling.api.domain.user.entity.AccountStatus;
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
import com.rolling.api.infra.apple.AppleTokenVerifier;
import com.rolling.api.infra.apple.dto.AppleUserResponse;
import com.rolling.api.infra.google.GoogleClient;
import com.rolling.api.infra.google.dto.GoogleUserResponse;
import com.rolling.api.infra.kakao.KakaoClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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
    private AppleTokenVerifier appleTokenVerifier;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserDeviceRepository userDeviceRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Spy
    private RefreshTokenHashProvider refreshTokenHashProvider = new RefreshTokenHashProvider();

    @Mock
    private AdminAccessConfig adminAccessConfig;

    @Mock
    private ScheduledTaskTracker scheduledTaskTracker;

    @Mock
    private OperationalAlertPublisher operationalAlertPublisher;

    @Mock
    private Clock clock;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void stubClock() {
        lenient().when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        lenient().when(clock.instant()).thenReturn(Instant.parse("2026-04-14T03:00:00Z"));
    }

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
        verify(refreshTokenRepository).flush();
        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().getTokenHash())
                .isEqualTo(refreshTokenHashProvider.hash("refresh-token"))
                .isNotEqualTo("refresh-token");
    }

    @Test
    @DisplayName("Apple 로그인은 identityToken 검증 결과로 신규 사용자를 생성하고 JWT를 발급한다")
    void login_appleCreatesNewUser() {
        SocialLoginRequest request = new SocialLoginRequest();
        ReflectionTestUtils.setField(request, "provider", "APPLE");
        ReflectionTestUtils.setField(request, "accessToken", "apple-identity-token");

        ReflectionTestUtils.setField(authService, "accessTokenExpiry", 1800000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiry", 1209600000L);

        AppleUserResponse appleUserResponse = new AppleUserResponse("apple-sub-1", "apple@example.com");
        User savedUser = User.builder()
                .socialId("apple-sub-1")
                .socialProvider(SocialProvider.APPLE)
                .nickname("Unknown")
                .email("apple@example.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(savedUser, "id", 70L);

        when(appleTokenVerifier.verify("apple-identity-token")).thenReturn(appleUserResponse);
        when(userRepository.findBySocialIdAndSocialProviderAndIsWithdrawnFalse("apple-sub-1", SocialProvider.APPLE))
                .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtTokenProvider.createAccessToken(70L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(70L)).thenReturn("refresh-token");
        when(adminAccessConfig.isAdmin(70L)).thenReturn(false);

        AuthResponse response = authService.login(request);

        assertThat(response.getUserId()).isEqualTo(70L);
        assertThat(response.getEmail()).isEqualTo("apple@example.com");
        assertThat(response.getName()).isEqualTo("Unknown");
        assertThat(response.isNewUser()).isTrue();
        verify(refreshTokenRepository).deleteByUserId(70L);
        verify(refreshTokenRepository).flush();
        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().getTokenHash())
                .isEqualTo(refreshTokenHashProvider.hash("refresh-token"))
                .isNotEqualTo("refresh-token");
    }

    @Test
    @DisplayName("동일한 Apple sub 재로그인은 기존 사용자를 반환한다")
    void login_appleExistingUser() {
        SocialLoginRequest request = new SocialLoginRequest();
        ReflectionTestUtils.setField(request, "provider", "APPLE");
        ReflectionTestUtils.setField(request, "accessToken", "apple-identity-token");

        ReflectionTestUtils.setField(authService, "accessTokenExpiry", 1800000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiry", 1209600000L);

        User existingUser = User.builder()
                .socialId("apple-sub-existing")
                .socialProvider(SocialProvider.APPLE)
                .nickname("Old Apple User")
                .email("old@example.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(existingUser, "id", 71L);

        when(appleTokenVerifier.verify("apple-identity-token"))
                .thenReturn(new AppleUserResponse("apple-sub-existing", "new@example.com"));
        when(userRepository.findBySocialIdAndSocialProviderAndIsWithdrawnFalse("apple-sub-existing", SocialProvider.APPLE))
                .thenReturn(Optional.of(existingUser));
        when(jwtTokenProvider.createAccessToken(71L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(71L)).thenReturn("refresh-token");
        when(adminAccessConfig.isAdmin(71L)).thenReturn(false);

        AuthResponse response = authService.login(request);

        assertThat(response.isNewUser()).isFalse();
        assertThat(response.getUserId()).isEqualTo(71L);
        assertThat(response.getName()).isEqualTo("Old Apple User");
        assertThat(response.getEmail()).isEqualTo("new@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("토큰 갱신 응답에 관리자 여부를 포함한다")
    void refresh_includesIsAdmin() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiry", 1800000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiry", 1209600000L);
        TokenRefreshRequest request = new TokenRefreshRequest();
        ReflectionTestUtils.setField(request, "refreshToken", "old-refresh-token");

        RefreshToken savedToken = RefreshToken.builder()
                .tokenHash(refreshTokenHashProvider.hash("old-refresh-token"))
                .userId(2L)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByTokenHash(refreshTokenHashProvider.hash("old-refresh-token")))
                .thenReturn(Optional.of(savedToken));
        when(jwtTokenProvider.validateToken("old-refresh-token")).thenReturn(true);
        User user = User.builder()
                .socialId("social-refresh")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("refresh-user")
                .email("refresh@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 2L);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(2L)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(2L)).thenReturn("new-refresh-token");
        when(adminAccessConfig.isAdmin(2L)).thenReturn(false);

        TokenRefreshResponse response = authService.refresh(request);

        assertThat(response.getIsAdmin()).isFalse();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        verify(refreshTokenRepository).delete(savedToken);
        verify(refreshTokenRepository).flush();
        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().getTokenHash())
                .isEqualTo(refreshTokenHashProvider.hash("new-refresh-token"))
                .isNotEqualTo("new-refresh-token");
    }

    @Test
    @DisplayName("legacy raw refresh token도 갱신에 성공하고 새 refresh token은 hash로 저장한다")
    void refresh_withLegacyRawToken_rotatesToHashedRefreshToken() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiry", 1800000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiry", 1209600000L);
        TokenRefreshRequest request = new TokenRefreshRequest();
        ReflectionTestUtils.setField(request, "refreshToken", "legacy-raw-refresh-token");

        RefreshToken savedToken = RefreshToken.builder()
                .tokenHash("legacy-raw-refresh-token")
                .userId(3L)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .build();

        when(refreshTokenRepository.findByTokenHash(refreshTokenHashProvider.hash("legacy-raw-refresh-token")))
                .thenReturn(Optional.empty());
        when(refreshTokenRepository.findByLegacyRawToken("legacy-raw-refresh-token"))
                .thenReturn(Optional.of(savedToken));
        when(jwtTokenProvider.validateToken("legacy-raw-refresh-token")).thenReturn(true);
        User user = User.builder()
                .socialId("social-legacy-refresh")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("legacy-refresh-user")
                .email("legacy-refresh@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 3L);
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(3L)).thenReturn("new-access-token");
        when(jwtTokenProvider.createRefreshToken(3L)).thenReturn("new-refresh-token");
        when(adminAccessConfig.isAdmin(3L)).thenReturn(false);

        TokenRefreshResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        verify(refreshTokenRepository).delete(savedToken);
        verify(refreshTokenRepository).flush();
        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        assertThat(refreshTokenCaptor.getValue().getTokenHash())
                .isEqualTo(refreshTokenHashProvider.hash("new-refresh-token"))
                .isNotEqualTo("new-refresh-token");
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

    @Test
    @DisplayName("장기 일시정지 사용자도 로그인할 수 있고 제재 상태를 응답으로 받는다")
    void login_suspendedUserReturnsRestrictedStatus() {
        SocialLoginRequest request = new SocialLoginRequest();
        ReflectionTestUtils.setField(request, "provider", "GOOGLE");
        ReflectionTestUtils.setField(request, "accessToken", "google-token");

        GoogleUserResponse googleUserResponse = new GoogleUserResponse();
        ReflectionTestUtils.setField(googleUserResponse, "sub", "social-suspended");
        ReflectionTestUtils.setField(googleUserResponse, "name", "정지사용자");
        ReflectionTestUtils.setField(googleUserResponse, "email", "suspended@example.com");

        User user = User.builder()
                .socialId("social-suspended")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("정지사용자")
                .email("suspended@example.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 3L);
        user.suspend(LocalDateTime.of(2126, 4, 20, 0, 0), "중대한 운영 위반");

        ReflectionTestUtils.setField(authService, "accessTokenExpiry", 1800000L);
        ReflectionTestUtils.setField(authService, "refreshTokenExpiry", 1209600000L);
        when(googleClient.getUserInfo("google-token")).thenReturn(googleUserResponse);
        when(userRepository.findBySocialIdAndSocialProviderAndIsWithdrawnFalse("social-suspended", SocialProvider.GOOGLE))
                .thenReturn(Optional.of(user));
        when(jwtTokenProvider.createAccessToken(3L)).thenReturn("access-token");
        when(jwtTokenProvider.createRefreshToken(3L)).thenReturn("refresh-token");
        when(adminAccessConfig.isAdmin(3L)).thenReturn(false);

        AuthResponse response = authService.login(request);

        assertThat(response.getAccountStatus()).isEqualTo(AccountStatus.SUSPENDED.name());
        assertThat(response.getSuspensionUntil()).isEqualTo(LocalDateTime.of(2126, 4, 20, 0, 0));
        assertThat(response.getSanctionReasonSummary()).isEqualTo("중대한 운영 위반");
    }
}
