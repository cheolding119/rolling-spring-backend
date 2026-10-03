package com.rolling.api.domain.achievement.dto;

import com.rolling.api.domain.achievement.entity.*;
import com.rolling.api.domain.user.entity.BeltColor;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TournamentAchievementResponse(
        Long id, String tournamentName, LocalDate competitionDate, AchievementResult result,
        UniformType uniformType, BeltColor beltColor, String ageDivision, DivisionType divisionType,
        String weightClass, String organizer, String resultUrl, String memo,
        LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static TournamentAchievementResponse from(TournamentAchievement entry) {
        return new TournamentAchievementResponse(entry.getId(), entry.getTournamentName(),
                entry.getCompetitionDate(), entry.getResult(), entry.getUniformType(), entry.getBeltColor(),
                entry.getAgeDivision(), entry.getDivisionType(), entry.getWeightClass(), entry.getOrganizer(),
                entry.getResultUrl(), entry.getMemo(), entry.getCreatedAt(), entry.getUpdatedAt());
    }
}
