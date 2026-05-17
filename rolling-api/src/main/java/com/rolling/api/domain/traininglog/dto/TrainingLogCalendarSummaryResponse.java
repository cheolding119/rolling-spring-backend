package com.rolling.api.domain.traininglog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "훈련 기록 연간 캘린더 요약 응답")
public class TrainingLogCalendarSummaryResponse {

    @Schema(description = "조회 연도", example = "2026")
    private Integer year;

    @Schema(description = "해당 연도의 총 훈련 시간(분)", example = "1320")
    private Integer totalTrainingMinutes;

    @Schema(description = "해당 연도의 활동 일수", example = "24")
    private Integer activeDays;

    @Schema(description = "월별 요약 목록")
    private List<TrainingLogCalendarMonthlySummary> monthlySummaries;

    @Schema(description = "일별 요약 목록")
    private List<TrainingLogCalendarDailySummary> dailySummaries;
}
