package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingLogLinkType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "훈련 기록 외부 링크")
public record TrainingLogExternalLink(
        @Schema(description = "링크 타입", example = "INSTAGRAM")
        TrainingLogLinkType type,
        @Schema(description = "정규화된 링크 URL", example = "https://www.instagram.com/p/xxxx/")
        String url
) {
}
