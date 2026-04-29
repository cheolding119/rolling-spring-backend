package com.rolling.api.global.exception;

import lombok.Getter;

@Getter
public class AuthException extends RuntimeException {

    private final String code;

    public AuthException(String code, String message) {
        super(message);
        this.code = code;
    }

    public static AuthException invalidCode() {
        return new AuthException("INVALID_CODE", "유효하지 않은 인증 코드입니다");
    }

    public static AuthException kakaoApiError(String message) {
        return new AuthException("KAKAO_API_ERROR", message);
    }

    public static AuthException googleApiError(String message) {
        return new AuthException("GOOGLE_API_ERROR", message);
    }

    public static AuthException appleApiError(String message) {
        return new AuthException("APPLE_API_ERROR", message);
    }

    public static AuthException unsupportedProvider(String provider) {
        return new AuthException("UNSUPPORTED_PROVIDER", "지원하지 않는 소셜 로그인 provider입니다: " + provider);
    }

    public static AuthException invalidToken() {
        return new AuthException("INVALID_TOKEN", "유효하지 않은 토큰입니다");
    }

    public static AuthException invalidRefreshToken() {
        return new AuthException("INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다");
    }

    public static AuthException expiredRefreshToken() {
        return new AuthException("EXPIRED_REFRESH_TOKEN", "만료된 Refresh Token입니다. 다시 로그인해 주세요");
    }
}
