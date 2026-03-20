package com.rolling.api.domain.auth.service;

import com.rolling.api.domain.auth.dto.SocialLoginRequest;
import com.rolling.api.domain.auth.repository.RefreshTokenRepository;
import com.rolling.api.domain.user.repository.UserDeviceRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.AuthException;
import com.rolling.api.global.security.AdminAccessConfig;
import com.rolling.api.global.security.jwt.JwtTokenProvider;
import com.rolling.api.infra.google.GoogleClient;
import com.rolling.api.infra.google.dto.GoogleUserResponse;
import com.rolling.api.infra.kakao.KakaoClient;
import com.rolling.api.infra.kakao.dto.KakaoUserResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginValidationTest {

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

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("구글 socialId(sub)가 없으면 GOOGLE_API_ERROR를 반환한다")
    void login_googleMissingSocialId() {
        SocialLoginRequest request = new SocialLoginRequest();
        ReflectionTestUtils.setField(request, "provider", "GOOGLE");
        ReflectionTestUtils.setField(request, "accessToken", "google-token");

        GoogleUserResponse response = new GoogleUserResponse();
        ReflectionTestUtils.setField(response, "sub", null);
        ReflectionTestUtils.setField(response, "name", "tester");
        ReflectionTestUtils.setField(response, "email", "tester@gmail.com");

        when(googleClient.getUserInfo("google-token")).thenReturn(response);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("GOOGLE_API_ERROR");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("카카오 socialId(id)가 없으면 KAKAO_API_ERROR를 반환한다")
    void login_kakaoMissingSocialId() {
        SocialLoginRequest request = new SocialLoginRequest();
        ReflectionTestUtils.setField(request, "provider", "KAKAO");
        ReflectionTestUtils.setField(request, "accessToken", "kakao-token");

        KakaoUserResponse response = new KakaoUserResponse();
        ReflectionTestUtils.setField(response, "id", null);

        when(kakaoClient.getUserInfo("kakao-token")).thenReturn(response);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .extracting("code")
                .isEqualTo("KAKAO_API_ERROR");

        verify(userRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
