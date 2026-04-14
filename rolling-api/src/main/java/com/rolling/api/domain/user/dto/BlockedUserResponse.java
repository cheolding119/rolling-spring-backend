package com.rolling.api.domain.user.dto;

import com.rolling.api.domain.user.entity.UserBlock;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "차단한 사용자 응답")
public class BlockedUserResponse {

    @Schema(description = "차단된 사용자 ID", example = "12")
    private Long userId;

    @Schema(description = "닉네임", example = "rolling_user")
    private String nickname;

    @Schema(description = "소속 체육관", example = "롤링짐 강남", nullable = true)
    private String affiliation;

    @Schema(description = "주짓수 벨트 색상", example = "BLUE")
    private String beltColor;

    @Schema(description = "차단 시각", example = "2026-04-14T13:00:00")
    private LocalDateTime blockedAt;

    public static BlockedUserResponse from(UserBlock userBlock) {
        return BlockedUserResponse.builder()
                .userId(userBlock.getBlockedUser().getId())
                .nickname(userBlock.getBlockedUser().getNickname())
                .affiliation(userBlock.getBlockedUser().getAffiliation())
                .beltColor(userBlock.getBlockedUser().getBeltColor().name())
                .blockedAt(userBlock.getBlockedAt())
                .build();
    }
}
