package com.rolling.api.domain.traininglog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "훈련 인사이트 기간 타입")
public enum TrainingLogInsightPeriod {
    WEEK,
    MONTH
}
