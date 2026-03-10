package com.rolling.api.domain.tournament.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TournamentCrawlResult {
    private int crawledCount;
    private int createdCount;
    private int updatedCount;
    private int skippedCount;
}
