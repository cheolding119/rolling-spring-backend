package com.rolling.api.domain.auth.service;

import com.rolling.api.domain.auth.dto.AuthResponse;
import com.rolling.api.domain.auth.dto.SocialLoginRequest;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final KakaoClient kakaoClient;
    private final GoogleClient googleClient;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.access-token-expiry}")
    private Long accessTokenExpiry;

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

        // DB에서 회원 조회 또는 생성
        boolean[] isNewUser = {false};
        User user = findOrCreateUser(socialId, provider, nickname, email, isNewUser);

        // JWT 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

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
                            .build();
                    User savedUser = userRepository.save(newUser);
                    log.info("New user created: {}", savedUser.getId());
                    return savedUser;
                });
    }
}