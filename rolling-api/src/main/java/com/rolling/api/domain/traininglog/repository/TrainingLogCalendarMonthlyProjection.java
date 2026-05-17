package com.rolling.api.domain.traininglog.repository;

public record TrainingLogCalendarMonthlyProjection(
        Integer month,
        Long totalMinutes,
        Long activeDays
) {
}
