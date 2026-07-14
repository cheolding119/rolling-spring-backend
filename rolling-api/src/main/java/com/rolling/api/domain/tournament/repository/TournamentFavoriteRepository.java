package com.rolling.api.domain.tournament.repository;

import com.rolling.api.domain.tournament.entity.TournamentFavorite;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TournamentFavoriteRepository extends JpaRepository<TournamentFavorite, Long> {

    @EntityGraph(attributePaths = "tournament")
    Optional<TournamentFavorite> findByUser_IdAndTournament_Id(Long userId, Long tournamentId);

    @EntityGraph(attributePaths = "tournament")
    List<TournamentFavorite> findAllByUser_Id(Long userId);

    @EntityGraph(attributePaths = {"user", "tournament"})
    @Query("""
            select favorite
            from TournamentFavorite favorite
            where favorite.notificationEnabled = true
              and favorite.scheduledAt is not null
              and favorite.sentAt is null
              and favorite.scheduledAt <= :targetTime
            order by favorite.scheduledAt asc, favorite.id asc
            """)
    List<TournamentFavorite> findDueReminderTargets(@Param("targetTime") LocalDateTime targetTime);

    List<TournamentFavorite> findAllByTournament_IdAndNotificationEnabledTrue(Long tournamentId);

    void deleteAllByTournament_Id(Long tournamentId);
}
