package com.rolling.api.domain.auth.service;

import com.rolling.api.domain.auth.dto.AuthResponse;
import com.rolling.api.domain.auth.dto.SocialLoginRequest;
import com.rolling.api.domain.auth.dto.TokenRefreshRequest;
import com.rolling.api.domain.auth.dto.TokenRefreshResponse;
import com.rolling.api.domain.auth.dto.WithdrawStatusResponse;
import com.rolling.api.domain.auth.entity.RefreshToken;
import com.rolling.api.domain.auth.repository.RefreshTokenRepository;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.AccountStatus;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserDeviceRepository;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.alert.OperationalAlertPublisher;
import com.rolling.api.global.exception.AuthException;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.monitoring.MonitoringTaskNames;
import com.rolling.api.global.monitoring.ScheduledTaskSnapshot;
import com.rolling.api.global.monitoring.ScheduledTaskTracker;
import com.rolling.api.global.security.AdminAccessConfig;
import com.rolling.api.global.security.jwt.JwtTokenProvider;
import com.rolling.api.infra.google.GoogleClient;
import com.rolling.api.infra.google.dto.GoogleUserResponse;
import com.rolling.api.infra.kakao.KakaoClient;
import com.rolling.api.infra.kakao.dto.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final ZoneId WITHDRAW_ZONE = ZoneId.of("Asia/Seoul");
    private static final LocalTime WITHDRAW_EXECUTION_TIME = LocalTime.of(21, 0);

    private final KakaoClient kakaoClient;
    private final GoogleClient googleClient;
    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AdminAccessConfig adminAccessConfig;
    private final ScheduledTaskTracker scheduledTaskTracker;
    private final OperationalAlertPublisher operationalAlertPublisher;
    private final Clock clock;

    @Value("${jwt.access-token-expiry}")
    private Long accessTokenExpiry;

    @Value("${jwt.refresh-token-expiry}")
    private Long refreshTokenExpiry;

    @Transactional
    public AuthResponse login(SocialLoginRequest request) {
        SocialProvider provider = parseProvider(request.getProvider());

        String socialId;
        String nickname;
        String email;

        switch (provider) {
            case KAKAO -> {
                KakaoUserResponse kakaoUser = kakaoClient.getUserInfo(request.getAccessToken());
                log.debug("Kakao user info received - id: {}, nickname: {}", kakaoUser.getId(), kakaoUser.getNickname());
                socialId = kakaoUser.getSocialId();
                nickname = kakaoUser.getNickname();
                email = kakaoUser.getEmail();
            }
            case GOOGLE -> {
                GoogleUserResponse googleUser = googleClient.getUserInfo(request.getAccessToken());
                log.debug("Google user info received - sub: {}, name: {}", googleUser.getSub(), googleUser.getNickname());
                socialId = googleUser.getSocialId();
                nickname = googleUser.getNickname();
                email = googleUser.getEmail();
            }
            default -> throw AuthException.unsupportedProvider(provider.name());
        }

        socialId = validateSocialId(provider, socialId);

        boolean[] isNewUser = {false};
        User user = findOrCreateUser(socialId, provider, nickname, email, isNewUser);
        LocalDateTime now = now();
        ensureLoginAllowed(user, now);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        refreshTokenRepository.deleteByUserId(user.getId());
        LocalDateTime expiryDate = LocalDateTime.now().plus(refreshTokenExpiry, ChronoUnit.MILLIS);
        refreshTokenRepository.save(RefreshToken.builder()
                .token(refreshToken)
                .userId(user.getId())
                .expiryDate(expiryDate)
                .build());

        log.info("{} login success - userId: {}, newUser: {}", provider, user.getId(), isNewUser[0]);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiry / 1000)
                .newUser(isNewUser[0])
                .userId(user.getId())
                .email(user.getEmail())
                .name(user.getNickname())
                .isAdmin(adminAccessConfig.isAdmin(user.getId()))
                .accountStatus(user.getEffectiveAccountStatus(now).name())
                .suspensionUntil(user.getEffectiveSuspensionUntil(now))
                .sanctionReasonSummary(user.getEffectiveSanctionReasonSummary(now))
                .build();
    }

    @Transactional
    public TokenRefreshResponse refresh(TokenRefreshRequest request) {
        String oldToken = request.getRefreshToken();

        RefreshToken savedToken = refreshTokenRepository.findByToken(oldToken)
                .orElseThrow(AuthException::invalidRefreshToken);

        if (savedToken.isExpired()) {
            refreshTokenRepository.delete(savedToken);
            throw AuthException.expiredRefreshToken();
        }

        if (!jwtTokenProvider.validateToken(oldToken)) {
            refreshTokenRepository.delete(savedToken);
            throw AuthException.invalidRefreshToken();
        }

        Long userId = savedToken.getUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AuthException.invalidRefreshToken());
        LocalDateTime now = now();
        ensureLoginAllowed(user, now);

        String newAccessToken = jwtTokenProvider.createAccessToken(userId);
        String newRefreshToken = jwtTokenProvider.createRefreshToken(userId);

        refreshTokenRepository.delete(savedToken);
        LocalDateTime expiryDate = LocalDateTime.now().plus(refreshTokenExpiry, ChronoUnit.MILLIS);
        refreshTokenRepository.save(RefreshToken.builder()
                .token(newRefreshToken)
                .userId(userId)
                .expiryDate(expiryDate)
                .build());

        log.info("Token refresh success - userId: {}", userId);

        return TokenRefreshResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(accessTokenExpiry / 1000)
                .isAdmin(adminAccessConfig.isAdmin(userId))
                .accountStatus(user.getEffectiveAccountStatus(now).name())
                .suspensionUntil(user.getEffectiveSuspensionUntil(now))
                .sanctionReasonSummary(user.getEffectiveSanctionReasonSummary(now))
                .build();
    }

    @Transactional
    public void logout(Long userId, String fcmToken) {
        if (StringUtils.hasText(fcmToken)) {
            removeUserDeviceToken(userId, fcmToken.trim());
        }
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Logout success - userId: {}", userId);
    }

    @Transactional
    public WithdrawStatusResponse withdraw(Long userId) {
        User user = userRepository.findByIdAndIsWithdrawnFalse(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        if (Boolean.TRUE.equals(user.getWithdrawalPending()) && user.getWithdrawalScheduledAt() != null) {
            return WithdrawStatusResponse.builder()
                    .withdrawalPending(true)
                    .scheduledAt(user.getWithdrawalScheduledAt())
                    .build();
        }

        LocalDateTime requestedAt = nowInWithdrawZone();
        LocalDateTime scheduledAt = requestedAt.toLocalDate()
                .plusDays(1)
                .atTime(WITHDRAW_EXECUTION_TIME);
        user.requestWithdrawal(requestedAt, scheduledAt);

        log.info("Withdraw requested - userId: {}, scheduledAt: {}", userId, scheduledAt);
        return WithdrawStatusResponse.builder()
                .withdrawalPending(true)
                .scheduledAt(scheduledAt)
                .build();
    }

    @Transactional
    public WithdrawStatusResponse cancelWithdraw(Long userId) {
        User user = userRepository.findByIdAndIsWithdrawnFalse(userId)
                .orElseThrow(() -> BusinessException.notFound("User not found"));

        if (!Boolean.TRUE.equals(user.getWithdrawalPending())) {
            throw new BusinessException("WITHDRAWAL_NOT_PENDING", "예약된 회원 탈퇴가 없습니다", HttpStatus.BAD_REQUEST);
        }

        user.cancelWithdrawal();
        log.info("Withdraw canceled - userId: {}", userId);

        return WithdrawStatusResponse.builder()
                .withdrawalPending(false)
                .scheduledAt(null)
                .build();
    }

    @Transactional
    @Scheduled(
            cron = "${auth.withdraw.schedule.cron:0 * * * * *}",
            zone = "${auth.withdraw.schedule.zone:Asia/Seoul}"
    )
    public void processScheduledWithdrawals() {
        scheduledTaskTracker.recordStart(MonitoringTaskNames.WITHDRAWAL_PROCESSOR);
        try {
            LocalDateTime now = nowInWithdrawZone();
            List<User> targets = userRepository
                    .findAllByIsWithdrawnFalseAndWithdrawalPendingTrueAndWithdrawalScheduledAtLessThanEqual(now);

            if (targets.isEmpty()) {
                scheduledTaskTracker.recordSuccess(MonitoringTaskNames.WITHDRAWAL_PROCESSOR, "processed=0");
                return;
            }

            for (User user : targets) {
                refreshTokenRepository.deleteByUserId(user.getId());
                userDeviceRepository.deleteAllByUser_Id(user.getId());
                user.withdraw();
                log.info("Withdraw executed - userId: {}", user.getId());
            }

            scheduledTaskTracker.recordSuccess(
                    MonitoringTaskNames.WITHDRAWAL_PROCESSOR,
                    "processed=" + targets.size()
            );
        } catch (Exception e) {
            scheduledTaskTracker.recordFailure(MonitoringTaskNames.WITHDRAWAL_PROCESSOR, e);
            ScheduledTaskSnapshot snapshot = scheduledTaskTracker.snapshot(MonitoringTaskNames.WITHDRAWAL_PROCESSOR);
            operationalAlertPublisher.publishSchedulerFailure(
                    MonitoringTaskNames.WITHDRAWAL_PROCESSOR,
                    snapshot.lastSummary(),
                    e
            );
            throw e;
        }
    }

    private SocialProvider parseProvider(String provider) {
        try {
            return SocialProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AuthException.unsupportedProvider(provider);
        }
    }

    private String validateSocialId(SocialProvider provider, String socialId) {
        if (socialId == null) {
            throw providerUserInfoError(provider, "소셜 사용자 식별자(socialId)가 없습니다");
        }

        String normalized = socialId.trim();
        if (normalized.isEmpty() || "null".equalsIgnoreCase(normalized)) {
            throw providerUserInfoError(provider, "소셜 사용자 식별자(socialId)가 유효하지 않습니다");
        }

        return normalized;
    }

    private AuthException providerUserInfoError(SocialProvider provider, String message) {
        return switch (provider) {
            case KAKAO -> AuthException.kakaoApiError(message);
            case GOOGLE -> AuthException.googleApiError(message);
        };
    }

    private LocalDateTime nowInWithdrawZone() {
        return ZonedDateTime.now(WITHDRAW_ZONE).toLocalDateTime();
    }

    private void removeUserDeviceToken(Long userId, String fcmToken) {
        userDeviceRepository.findByUser_IdAndFcmToken(userId, fcmToken)
                .ifPresent(userDevice -> {
                    userDevice.assignUser(null);
                    userDeviceRepository.delete(userDevice);
                    log.info("Removed FCM token during auth lifecycle. userId={}, tokenPrefix={}", userId, summarizeToken(fcmToken));
                });
    }

    private String summarizeToken(String token) {
        if (!StringUtils.hasText(token)) {
            return "<blank>";
        }

        int prefixLength = Math.min(12, token.length());
        return token.substring(0, prefixLength) + (token.length() > prefixLength ? "..." : "");
    }

    private User findOrCreateUser(String socialId, SocialProvider provider, String nickname, String email, boolean[] isNewUser) {
        return userRepository.findBySocialIdAndSocialProviderAndIsWithdrawnFalse(socialId, provider)
                .map(existingUser -> {
                    existingUser.updateNickname(nickname);
                    existingUser.updateEmail(email);
                    log.debug("Existing user updated: {}", existingUser.getId());
                    return existingUser;
                })
                .orElseGet(() -> {
                    isNewUser[0] = true;
                    User newUser = User.builder()
                            .socialId(socialId)
                            .socialProvider(provider)
                            .nickname(nickname)
                            .email(email)
                            .beltColor(BeltColor.WHITE)
                            .build();
                    User savedUser = userRepository.save(newUser);
                    log.info("New user created: {}", savedUser.getId());
                    return savedUser;
                });
    }

    private void ensureLoginAllowed(User user, LocalDateTime now) {
        AccountStatus effectiveStatus = user.getEffectiveAccountStatus(now);
        if (effectiveStatus == AccountStatus.WITHDRAWN) {
            throw BusinessException.forbidden("이용이 정지된 계정입니다");
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
