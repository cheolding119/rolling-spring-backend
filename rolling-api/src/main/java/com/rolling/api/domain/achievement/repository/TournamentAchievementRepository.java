package com.rolling.api.domain.achievement.repository;

import com.rolling.api.domain.achievement.entity.AchievementResult;
import com.rolling.api.domain.achievement.entity.TournamentAchievement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TournamentAchievementRepository extends JpaRepository<TournamentAchievement, Long> {
    Page<TournamentAchievement> findAllByUser_Id(Long userId, Pageable pageable);

    Optional<TournamentAchievement> findByIdAndUser_Id(Long id, Long userId);

    @Query("select a.result as result, count(a) as total from TournamentAchievement a "
            + "where a.user.id = :userId group by a.result")
    List<ResultCount> countResults(@Param("userId") Long userId);

    @Modifying
    @Query("delete from TournamentAchievement a where a.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    interface ResultCount {
        AchievementResult getResult();
        long getTotal();
    }
}
