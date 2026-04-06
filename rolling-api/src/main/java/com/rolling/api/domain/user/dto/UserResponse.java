package com.rolling.api.domain.user.dto;

import com.rolling.api.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "사용자 정보 응답")
public class UserResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "닉네임", example = "rolling_user")
    private String nickname;

    @Schema(description = "이메일", example = "user@gmail.com")
    private String email;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;

    @Schema(description = "소셜 로그인 제공자", example = "GOOGLE")
    private String socialProvider;

    @Schema(description = "주짓수 벨트 색상", example = "BLUE")
    private String beltColor;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "회원 탈퇴 예약 상태", example = "false")
    private Boolean withdrawalPending;

    @Schema(description = "회원 탈퇴 예정 시각 (예약된 경우)", example = "2026-03-02T21:00:00")
    private LocalDateTime withdrawalScheduledAt;

    @Schema(description = "관리자 여부", example = "false")
    private Boolean isAdmin;

    @Schema(description = "사용자 설정")
    private UserSettingsResponse settings;

    public static UserResponse from(User user, boolean isAdmin) {
        return UserResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .socialProvider(user.getSocialProvider().name())
                .beltColor(user.getBeltColor().name())
                .createdAt(user.getCreatedAt())
                .withdrawalPending(user.getWithdrawalPending())
                .withdrawalScheduledAt(user.getWithdrawalScheduledAt())
                .isAdmin(isAdmin)
                .settings(UserSettingsResponse.from(user))
                .build();
    }
}
