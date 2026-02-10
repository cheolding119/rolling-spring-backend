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
}
