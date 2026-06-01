package com.rolling.api.domain.user.dto;

import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.entity.AccountStatus;
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

    @Schema(description = "소속 체육관", example = "롤링짐 강남", nullable = true)
    private String affiliation;

    @Schema(description = "소셜 로그인 제공자", example = "GOOGLE")
    private String socialProvider;

    @Schema(description = "주짓수 벨트 색상", example = "BLUE")
    private String beltColor;

    @Schema(description = "그랄 수", example = "2", nullable = true)
    private Integer stripeCount;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "회원 탈퇴 예약 상태", example = "false")
    private Boolean withdrawalPending;

    @Schema(description = "회원 탈퇴 예정 시각 (예약된 경우)", example = "2026-03-02T21:00:00")
    private LocalDateTime withdrawalScheduledAt;

    @Schema(description = "계정 상태", example = "ACTIVE")
    private AccountStatus accountStatus;

    @Schema(description = "일시정지 종료 시각", example = "2026-04-20T00:00:00", nullable = true)
    private LocalDateTime suspensionUntil;

    @Schema(description = "제재 사유 요약", example = "반복적인 욕설", nullable = true)
    private String sanctionReasonSummary;

    @Schema(description = "관리자 여부", example = "false")
    private Boolean isAdmin;

    @Schema(description = "사용자 설정")
    private UserSettingsResponse settings;

    public static UserResponse from(User user, boolean isAdmin, LocalDateTime now) {
        return UserResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .affiliation(user.getAffiliation())
                .socialProvider(user.getSocialProvider().name())
                .beltColor(user.getBeltColor().name())
                .stripeCount(user.getStripeCount())
                .createdAt(user.getCreatedAt())
                .withdrawalPending(user.getWithdrawalPending())
                .withdrawalScheduledAt(user.getWithdrawalScheduledAt())
                .accountStatus(user.getEffectiveAccountStatus(now))
                .suspensionUntil(user.getEffectiveSuspensionUntil(now))
                .sanctionReasonSummary(user.getEffectiveSanctionReasonSummary(now))
                .isAdmin(isAdmin)
                .settings(UserSettingsResponse.from(user))
                .build();
    }
}
