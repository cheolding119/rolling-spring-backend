package com.rolling.api.domain.tournament.crawler;

import com.rolling.api.domain.tournament.model.TournamentModel;

import java.util.List;

public interface TournamentCrawler {
    List<TournamentModel> crawlAll();
}
