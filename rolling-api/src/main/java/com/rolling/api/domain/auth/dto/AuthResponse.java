package com.rolling.api.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "인증 응답")
public class AuthResponse {

    @Schema(description = "JWT Access Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String accessToken;

    @Schema(description = "JWT Refresh Token", example = "eyJhbGciOiJIUzI1NiJ9...")
    private String refreshToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    private String tokenType;

    @Schema(description = "Access Token 만료 시간 (초)", example = "1800")
    private Long expiresIn;

    @Schema(description = "신규 회원 여부", example = "true")
    private boolean newUser;

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    @Schema(description = "사용자 이메일", example = "user@gmail.com")
    private String email;

    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;

    @Schema(description = "관리자 여부", example = "false")
    private Boolean isAdmin;

    @Schema(description = "계정 상태", example = "ACTIVE")
    private String accountStatus;

    @Schema(description = "일시정지 종료 시각", example = "2026-04-20T00:00:00", nullable = true)
    private LocalDateTime suspensionUntil;

    @Schema(description = "제재 사유 요약", example = "반복적인 욕설", nullable = true)
    private String sanctionReasonSummary;
}
