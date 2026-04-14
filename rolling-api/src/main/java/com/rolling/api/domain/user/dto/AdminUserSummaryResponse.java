package com.rolling.api.domain.user.dto;

import com.rolling.api.domain.user.entity.AccountStatus;
import com.rolling.api.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "관리자 사용자 목록 응답")
public class AdminUserSummaryResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "닉네임", example = "rolling_user")
    private String nickname;

    @Schema(description = "이메일", example = "user@gmail.com", nullable = true)
    private String email;

    @Schema(description = "소속 체육관", example = "롤링짐 강남", nullable = true)
    private String affiliation;

    @Schema(description = "생성 시각", example = "2026-04-14T13:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "계정 상태", example = "ACTIVE")
    private AccountStatus accountStatus;

    @Schema(description = "일시정지 종료 시각", example = "2026-04-20T00:00:00", nullable = true)
    private LocalDateTime suspensionUntil;

    @Schema(description = "최근 제재 시각", example = "2026-04-14T13:00:00", nullable = true)
    private LocalDateTime lastSanctionAt;

    public static AdminUserSummaryResponse from(User user, LocalDateTime now, LocalDateTime lastSanctionAt) {
        return AdminUserSummaryResponse.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .affiliation(user.getAffiliation())
                .createdAt(user.getCreatedAt())
                .accountStatus(user.getEffectiveAccountStatus(now))
                .suspensionUntil(user.getEffectiveSuspensionUntil(now))
                .lastSanctionAt(lastSanctionAt)
                .build();
    }
}
