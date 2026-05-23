package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "훈련 인사이트 일별 데이터")
public record TrainingLogDailyInsight(
        LocalDate date,
        Integer recordCount,
        Boolean gymAttendance,
        Integer totalTrainingMinutes,
        Double averageTrainingIntensity,
        Double averageCondition,
        List<TrainingLogCategory> categories
) {
}
