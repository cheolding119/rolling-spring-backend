package com.rolling.api.domain.tournament.crawler;

import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.model.TournamentModel;

import java.util.List;

public interface TournamentCrawler {
    TournamentSource getSource();

    List<TournamentModel> crawlAll();
}