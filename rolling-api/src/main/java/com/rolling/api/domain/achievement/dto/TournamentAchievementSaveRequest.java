package com.rolling.api.domain.achievement.dto;

import com.rolling.api.domain.achievement.entity.AchievementResult;
import com.rolling.api.domain.achievement.entity.DivisionType;
import com.rolling.api.domain.achievement.entity.UniformType;
import com.rolling.api.domain.user.entity.BeltColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record TournamentAchievementSaveRequest(
        @NotBlank @Size(max = 255) String tournamentName,
        @NotNull LocalDate competitionDate,
        @NotNull AchievementResult result,
        UniformType uniformType,
        BeltColor beltColor,
        @Size(max = 100) String ageDivision,
        DivisionType divisionType,
        @Size(max = 100) String weightClass,
        @Size(max = 255) String organizer,
        @Size(max = 1000) String resultUrl,
        @Size(max = 500) String memo
) {
    public TournamentAchievementSaveRequest {
        tournamentName = normalize(tournamentName);
        ageDivision = normalize(ageDivision);
        weightClass = normalize(weightClass);
        organizer = normalize(organizer);
        resultUrl = normalize(resultUrl);
        memo = normalize(memo);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
