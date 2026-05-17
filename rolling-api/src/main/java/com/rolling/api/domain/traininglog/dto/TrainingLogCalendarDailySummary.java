package com.rolling.api.domain.traininglog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "훈련 기록 일별 요약")
public record TrainingLogCalendarDailySummary(
        @Schema(description = "훈련 날짜", example = "2026-05-17")
        LocalDate date,
        @Schema(description = "해당 일자의 총 훈련 시간(분)", example = "120")
        Integer totalMinutes,
        @Schema(description = "해당 일자의 기록 수", example = "2")
        Integer recordCount
) {
}
