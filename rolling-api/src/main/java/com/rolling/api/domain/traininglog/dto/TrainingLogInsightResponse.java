package com.rolling.api.domain.traininglog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(description = "훈련 주간/월간 인사이트 응답")
public class TrainingLogInsightResponse {

    private TrainingLogInsightPeriod period;
    private LocalDate startDate;
    private LocalDate endDate;
    private TrainingLogInsightSummary summary;
    private List<TrainingLogDailyInsight> dailyStats;
    private List<TrainingLogCategoryInsight> categoryBreakdown;
    private List<TrainingLogHashtagInsight> topHashtags;
}
