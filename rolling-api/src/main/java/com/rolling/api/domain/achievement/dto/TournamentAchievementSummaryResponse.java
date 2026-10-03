package com.rolling.api.domain.achievement.dto;

public record TournamentAchievementSummaryResponse(
        long recordCount, long goldCount, long silverCount, long bronzeCount,
        long medalCount, long participationCount
) {
}
