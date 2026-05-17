package com.rolling.api.domain.traininglog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "훈련 기록 월별 요약")
public record TrainingLogCalendarMonthlySummary(
        @Schema(description = "월", example = "5")
        Integer month,
        @Schema(description = "해당 월의 총 훈련 시간(분)", example = "420")
        Integer totalMinutes,
        @Schema(description = "해당 월의 활동 일수", example = "8")
        Integer activeDays
) {
}
