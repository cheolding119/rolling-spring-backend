package com.rolling.api.domain.user.dto;

import com.rolling.api.domain.user.entity.UserSanction;
import com.rolling.api.domain.user.entity.UserSanctionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "관리자 사용자 제재 응답")
public class UserSanctionResponse {

    @Schema(description = "제재 ID", example = "100")
    private Long id;

    @Schema(description = "제재 타입", example = "TEMP_SUSPEND")
    private UserSanctionType type;

    @Schema(description = "제재 사유", example = "반복적인 욕설")
    private String reason;

    @Schema(description = "운영 메모", example = "커뮤니티 규정 위반", nullable = true)
    private String memo;

    @Schema(description = "제재 시작 시각", example = "2026-04-14T13:00:00")
    private LocalDateTime startsAt;

    @Schema(description = "제재 종료 시각", example = "2026-04-20T00:00:00", nullable = true)
    private LocalDateTime endsAt;

    @Schema(description = "생성한 관리자 ID", example = "1")
    private Long createdByUserId;

    @Schema(description = "생성 시각", example = "2026-04-14T13:00:00")
    private LocalDateTime createdAt;

    @Schema(description = "해제한 관리자 ID", example = "1", nullable = true)
    private Long releasedByUserId;

    @Schema(description = "해제 시각", example = "2026-04-20T00:00:00", nullable = true)
    private LocalDateTime releasedAt;

    public static UserSanctionResponse from(UserSanction sanction) {
        return UserSanctionResponse.builder()
                .id(sanction.getId())
                .type(sanction.getSanctionTypeEnum())
                .reason(sanction.getReason())
                .memo(sanction.getMemo())
                .startsAt(sanction.getStartsAt())
                .endsAt(sanction.getEndsAt())
                .createdByUserId(sanction.getCreatedByUserId())
                .createdAt(sanction.getCreatedAt())
                .releasedByUserId(sanction.getReleasedByUserId())
                .releasedAt(sanction.getReleasedAt())
                .build();
    }
}
