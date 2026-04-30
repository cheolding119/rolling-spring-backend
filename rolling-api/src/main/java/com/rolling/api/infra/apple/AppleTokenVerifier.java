package com.rolling.api.infra.apple;

import com.rolling.api.global.exception.AuthException;
import com.rolling.api.infra.apple.dto.AppleUserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtIssuerValidator;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleTokenVerifier {

    private final AppleAuthProperties properties;
    private volatile JwtDecoder jwtDecoder;

    public AppleUserResponse verify(String identityToken) {
        if (!StringUtils.hasText(identityToken)) {
            throw AuthException.appleApiError("Apple identityToken이 없습니다");
        }
        if (!StringUtils.hasText(properties.getClientId())) {
            throw AuthException.appleApiError("Apple client_id 설정이 없습니다");
        }

        try {
            Jwt jwt = decoder().decode(identityToken);
            return new AppleUserResponse(jwt.getSubject(), jwt.getClaimAsString("email"));
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Apple identityToken verification failed: {}", e.getMessage());
            throw AuthException.appleApiError("유효하지 않은 Apple identityToken입니다");
        }
    }

    private JwtDecoder decoder() {
        JwtDecoder current = jwtDecoder;
        if (current != null) {
            return current;
        }

        synchronized (this) {
            if (jwtDecoder == null) {
                NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(properties.getJwksUrl()).build();
                decoder.setJwtValidator(validator());
                jwtDecoder = decoder;
            }
            return jwtDecoder;
        }
    }

    private OAuth2TokenValidator<Jwt> validator() {
        return new DelegatingOAuth2TokenValidator<>(
                new JwtTimestampValidator(),
                new JwtIssuerValidator(properties.getIssuer()),
                audienceValidator()
        );
    }

    private OAuth2TokenValidator<Jwt> audienceValidator() {
        return jwt -> {
            if (jwt.getAudience().contains(properties.getClientId())) {
                return OAuth2TokenValidatorResult.success();
            }

            OAuth2Error error = new OAuth2Error(
                    "invalid_token",
                    "Apple identityToken audience does not match configured client_id",
                    null
            );
            return OAuth2TokenValidatorResult.failure(error);
        };
    }
}
