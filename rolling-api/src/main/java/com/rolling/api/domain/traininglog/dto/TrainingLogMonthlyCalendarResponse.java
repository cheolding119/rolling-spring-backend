package com.rolling.api.domain.traininglog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "훈련 기록 월간 캘린더 요약 응답")
public class TrainingLogMonthlyCalendarResponse {

    @Schema(description = "조회 연도", example = "2026")
    private Integer year;

    @Schema(description = "조회 월", example = "5")
    private Integer month;

    @Schema(description = "일별 요약 목록")
    private List<TrainingLogMonthlyCalendarDailySummary> dailySummaries;
}
