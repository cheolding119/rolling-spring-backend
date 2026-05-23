package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "훈련 출석 잔디 일별 데이터")
public record TrainingLogAttendanceGrassDay(
        @Schema(description = "날짜", example = "2026-05-22")
        LocalDate date,
        @Schema(description = "요일", example = "FRI")
        String dayOfWeek,
        @Schema(description = "체육관 출석 여부", example = "true")
        Boolean attended,
        @Schema(description = "잔디 농도. 0은 출석 없음, 1~3은 훈련 시간 기준 활성 단계", example = "2")
        Integer level,
        @Schema(description = "해당 날짜 기록 수", example = "1")
        Integer recordCount,
        @Schema(description = "해당 날짜 총 훈련 시간(분)", example = "90")
        Integer totalTrainingMinutes,
        @Schema(description = "해당 날짜 평균 훈련 강도", example = "3.0", nullable = true)
        Double averageTrainingIntensity,
        @Schema(description = "해당 날짜 평균 컨디션", example = "4.0", nullable = true)
        Double averageCondition,
        @Schema(description = "해당 날짜 카테고리 목록")
        List<TrainingLogCategory> categories
) {
}
