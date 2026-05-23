package com.rolling.api.domain.traininglog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "훈련 출석 365일 잔디 응답")
public class TrainingLogAttendanceGrassResponse {

    @Schema(description = "조회 시작일", example = "2025-05-23")
    private LocalDate startDate;

    @Schema(description = "조회 종료일", example = "2026-05-22")
    private LocalDate endDate;

    @Schema(description = "조회 일수", example = "365")
    private Integer totalDays;

    @Schema(description = "365일 범위 내 출석일 수", example = "128")
    private Integer attendanceDays;

    @Schema(description = "현재 연속 출석일 수", example = "3")
    private Integer currentStreakDays;

    @Schema(description = "365일 범위 내 최장 연속 출석일 수", example = "14")
    private Integer longestStreakDays;

    @Schema(description = "최근 30일 출석일 수", example = "12")
    private Integer recent30DaysAttendanceDays;

    @Schema(description = "365일 일별 잔디 데이터")
    private List<TrainingLogAttendanceGrassDay> days;
}
