package com.rolling.api.domain.traininglog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "훈련 인사이트 기간 요약")
public class TrainingLogInsightSummary {

    private Integer recordCount;
    private Integer trainingDays;
    private Integer attendanceDays;
    private Double attendanceRate;
    private Integer totalTrainingMinutes;
    private Double averageTrainingMinutesPerAttendanceDay;
    private Double averageTrainingIntensity;
    private Double averageCondition;
    private Double checklistCompletionRate;
}
