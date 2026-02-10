package com.rolling.api.domain.auth.service;

import com.rolling.api.domain.auth.dto.AuthResponse;
import com.rolling.api.domain.auth.dto.KakaoLoginRequest;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.security.jwt.JwtTokenProvider;
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
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${jwt.access-token-expiry}")
    private Long accessTokenExpiry;

    @Transactional
    public AuthResponse login(KakaoLoginRequest request) {
        // 1. Flutter에서 받은 accessToken으로 카카오 사용자 정보 직접 조회
        KakaoUserResponse userResponse = kakaoClient.getUserInfo(request.getAccessToken());
        log.debug("Kakao user info received - id: {}, nickname: {}", userResponse.getId(), userResponse.getNickname());

        // 2. DB에서 회원 조회 또는 생성
        boolean[] isNewUser = {false};
        User user = findOrCreateUser(userResponse, isNewUser);

        // 3. JWT 발급
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        log.info("Kakao login success - userId: {}, newUser: {}", user.getId(), isNewUser[0]);

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

    private User findOrCreateUser(KakaoUserResponse kakaoUser, boolean[] isNewUser) {
        String socialId = kakaoUser.getSocialId();

        return userRepository.findBySocialIdAndSocialProvider(socialId, SocialProvider.KAKAO)
                .map(existingUser -> {
                    existingUser.updateNickname(kakaoUser.getNickname());
                    existingUser.updateEmail(kakaoUser.getEmail());
                    log.debug("Existing user updated: {}", existingUser.getId());
                    return existingUser;
                })
                .orElseGet(() -> {
                    isNewUser[0] = true;
                    User newUser = User.builder()
                            .socialId(socialId)
                            .socialProvider(SocialProvider.KAKAO)
                            .nickname(kakaoUser.getNickname())
                            .email(kakaoUser.getEmail())
                            .build();
                    User savedUser = userRepository.save(newUser);
                    log.info("New user created: {}", savedUser.getId());
                    return savedUser;
                });
    }
}
