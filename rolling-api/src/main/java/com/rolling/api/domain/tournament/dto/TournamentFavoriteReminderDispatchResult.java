package com.rolling.api.domain.tournament.dto;

public record TournamentFavoriteReminderDispatchResult(
        int scannedCount,
        int sentCount,
        int disabledCount,
        int skippedCount
) {
}
