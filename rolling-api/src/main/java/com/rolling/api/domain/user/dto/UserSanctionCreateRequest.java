package com.rolling.api.domain.user.dto;

import com.rolling.api.domain.user.entity.UserSanctionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "관리자 사용자 제재 생성 요청")
public class UserSanctionCreateRequest {

    @NotNull
    @Schema(description = "제재 타입", example = "TEMP_SUSPEND")
    private UserSanctionType type;

    @Schema(description = "제재 사유", example = "반복적인 욕설", nullable = true)
    private String reason;

    @Schema(description = "운영 메모", example = "커뮤니티 규정 위반", nullable = true)
    private String memo;

    @Schema(description = "일시정지 종료 시각", example = "2026-04-20T00:00:00", nullable = true)
    private LocalDateTime endsAt;
}
