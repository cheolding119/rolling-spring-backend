package com.rolling.api.domain.traininglog.repository;

import java.time.LocalDate;

public record TrainingLogCalendarDailyProjection(
        LocalDate date,
        Long totalMinutes,
        Long recordCount
) {
}
