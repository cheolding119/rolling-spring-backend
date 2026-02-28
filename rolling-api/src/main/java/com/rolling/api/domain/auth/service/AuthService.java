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
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.AuthException;
import com.rolling.api.global.security.jwt.JwtTokenProvider;
import com.rolling.api.infra.google.GoogleClient;
import com.rolling.api.infra.google.dto.GoogleUserResponse;
import com.rolling.api.infra.kakao.KakaoClient;
import com.rolling.api.infra.kakao.dto.KakaoUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoClient kakaoClient;
    private final GoogleClient googleClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

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

        boolean[] isNewUser = {false};
        User user = findOrCreateUser(socialId, provider, nickname, email, isNewUser);

        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 기존 RefreshToken 삭제 후 새로 저장 (한 계정 = 하나의 RefreshToken)
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

        // Refresh Token Rotation: 새 토큰 발급 후 기존 토큰 교체
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
                .build();
    }

    @Transactional
    public void logout(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("Logout success - userId: {}", userId);
    }

    private SocialProvider parseProvider(String provider) {
        try {
            return SocialProvider.valueOf(provider.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw AuthException.unsupportedProvider(provider);
        }
    }

    private User findOrCreateUser(String socialId, SocialProvider provider, String nickname, String email, boolean[] isNewUser) {
        return userRepository.findBySocialIdAndSocialProvider(socialId, provider)
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
}
