package com.rolling.api.domain.auth.service;

import com.rolling.api.domain.auth.dto.WithdrawStatusResponse;
import com.rolling.api.domain.auth.repository.RefreshTokenRepository;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserDeviceRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceWithdrawTest {

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
    @DisplayName("회원 탈퇴 요청 시 다음날 21:00으로 예약되고 즉시 탈퇴는 실행되지 않는다")
    void withdraw_requestSchedules() {
        User user = User.builder()
                .socialId("social-123")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("rolling_user")
                .email("user@test.com")
                .phone("010-1111-2222")
                .beltColor(BeltColor.BLUE)
                .fcmToken("fcm-token")
                .build();
        ReflectionTestUtils.setField(user, "id", 10L);

        when(userRepository.findByIdAndIsWithdrawnFalse(10L)).thenReturn(Optional.of(user));

        WithdrawStatusResponse response = authService.withdraw(10L);

        assertThat(response.isWithdrawalPending()).isTrue();
        assertThat(response.getScheduledAt()).isNotNull();
        assertThat(response.getScheduledAt().toLocalTime()).isEqualTo(java.time.LocalTime.of(21, 0));
        assertThat(user.getWithdrawalPending()).isTrue();
        assertThat(user.getWithdrawalScheduledAt()).isNotNull();
        assertThat(user.getIsWithdrawn()).isFalse();
        verify(refreshTokenRepository, never()).deleteByUserId(10L);
        verify(userDeviceRepository, never()).deleteAllByUser_Id(10L);
    }

    @Test
    @DisplayName("회원이 존재하지 않으면 NOT_FOUND 예외가 발생한다")
    void withdraw_userNotFound() {
        when(userRepository.findByIdAndIsWithdrawnFalse(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.withdraw(99L))
                .isInstanceOf(BusinessException.class)
                .extracting("code")
                .isEqualTo("NOT_FOUND");
    }

    @Test
    @DisplayName("회원 탈퇴 취소 시 예약 상태가 해제된다")
    void cancelWithdraw_success() {
        User user = User.builder()
                .socialId("social-321")
                .socialProvider(SocialProvider.KAKAO)
                .nickname("rolling_user")
                .email("user@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", 20L);
        user.requestWithdrawal(LocalDateTime.now(), LocalDateTime.now().plusDays(1).withHour(21).withMinute(0).withSecond(0).withNano(0));

        when(userRepository.findByIdAndIsWithdrawnFalse(20L)).thenReturn(Optional.of(user));

        WithdrawStatusResponse response = authService.cancelWithdraw(20L);

        assertThat(response.isWithdrawalPending()).isFalse();
        assertThat(response.getScheduledAt()).isNull();
        assertThat(user.getWithdrawalPending()).isFalse();
        assertThat(user.getWithdrawalScheduledAt()).isNull();
    }

    @Test
    @DisplayName("예약 시각이 지난 회원은 배치 실행 시 최종 탈퇴 처리된다")
    void processScheduledWithdrawals_executesWithdraw() {
        User user = User.builder()
                .socialId("social-777")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("rolling_user")
                .email("user@test.com")
                .phone("010-1111-2222")
                .beltColor(BeltColor.BLUE)
                .fcmToken("fcm-token")
                .build();
        ReflectionTestUtils.setField(user, "id", 30L);
        user.requestWithdrawal(
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().minusMinutes(1)
        );

        when(userRepository.findAllByIsWithdrawnFalseAndWithdrawalPendingTrueAndWithdrawalScheduledAtLessThanEqual(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(user));

        authService.processScheduledWithdrawals();

        verify(refreshTokenRepository).deleteByUserId(30L);
        verify(userDeviceRepository).deleteAllByUser_Id(30L);
        assertThat(user.getIsWithdrawn()).isTrue();
        assertThat(user.getWithdrawalPending()).isFalse();
        assertThat(user.getEmail()).isNull();
        assertThat(user.getPhone()).isNull();
        assertThat(user.getDevices()).isEmpty();
        assertThat(user.getFcmToken()).isNull();
    }
}
