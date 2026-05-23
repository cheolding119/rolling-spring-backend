package com.rolling.api.domain.traininglog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "훈련 인사이트 상위 해시태그")
public record TrainingLogHashtagInsight(
        String tag,
        Integer count
) {
}
