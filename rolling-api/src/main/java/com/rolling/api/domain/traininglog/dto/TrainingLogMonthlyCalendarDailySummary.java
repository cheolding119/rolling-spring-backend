package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingLogColor;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "훈련 기록 월간 캘린더 일별 요약")
public record TrainingLogMonthlyCalendarDailySummary(
        @Schema(description = "훈련 날짜", example = "2026-05-17")
        LocalDate date,
        @Schema(description = "해당 일자의 색상 목록")
        List<TrainingLogColor> colors,
        @Schema(description = "해당 일자의 카테고리 목록")
        List<TrainingLogCategory> categories,
        @Schema(description = "해당 일자의 기록 수", example = "2")
        Integer recordCount
) {
}
