package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "훈련 인사이트 카테고리 분포")
public record TrainingLogCategoryInsight(
        TrainingLogCategory category,
        Integer recordCount,
        Integer totalTrainingMinutes
) {
}
