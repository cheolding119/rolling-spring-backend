package com.rolling.api.domain.tournament.repository;

import com.rolling.api.domain.tournament.entity.Tournament;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TournamentRepository extends JpaRepository<Tournament, Long> {

    @Query(
            value = """
                    SELECT t FROM Tournament t
                    WHERE (:source IS NULL OR t.source = :source OR (:includeLegacyManual = true AND t.source IS NULL))
                      AND (
                            :keyword IS NULL
                            OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(t.organizer, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(t.location, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    ORDER BY
                      CASE
                        WHEN t.registrationDeadline IS NOT NULL AND t.registrationDeadline < :todayIso THEN 1
                        ELSE 0
                      END ASC,
                      CASE
                        WHEN t.registrationDeadline IS NULL THEN 1
                        ELSE 0
                      END ASC,
                      t.registrationDeadline ASC,
                      CASE
                        WHEN t.competitionDate IS NULL THEN 1
                        ELSE 0
                      END ASC,
                      t.competitionDate ASC,
                      t.id ASC
                    """,
            countQuery = """
                    SELECT COUNT(t) FROM Tournament t
                    WHERE (:source IS NULL OR t.source = :source OR (:includeLegacyManual = true AND t.source IS NULL))
                      AND (
                            :keyword IS NULL
                            OR LOWER(t.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(t.organizer, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR LOWER(COALESCE(t.location, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """
    )
    Page<Tournament> searchVisible(
            @Param("source") TournamentSource source,
            @Param("includeLegacyManual") boolean includeLegacyManual,
            @Param("keyword") String keyword,
            @Param("todayIso") String todayIso,
            Pageable pageable
    );

    Optional<Tournament> findByApplyLink(String applyLink);

    Optional<Tournament> findByTitleAndCompetitionDate(String title, String competitionDate);
}