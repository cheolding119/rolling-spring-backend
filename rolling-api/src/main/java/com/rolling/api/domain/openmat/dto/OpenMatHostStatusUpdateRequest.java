package com.rolling.api.domain.openmat.dto;

import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "오픈매트 작성자 모집 상태 수동 변경 요청")
public class OpenMatHostStatusUpdateRequest {

    @NotNull(message = "status는 필수입니다")
    @Schema(description = "수동 변경할 모집 상태. RECRUITING 또는 CLOSED만 허용합니다.", example = "CLOSED")
    private OpenMatStatus status;
}
