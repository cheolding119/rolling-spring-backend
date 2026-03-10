package com.rolling.api.domain.tournament.repository;

import com.rolling.api.domain.tournament.entity.Tournament;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    Optional<Tournament> findByApplyLink(String applyLink);

    Optional<Tournament> findByTitleAndCompetitionDate(String title, String competitionDate);
}
