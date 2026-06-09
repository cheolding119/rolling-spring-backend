package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingCardFavorite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainingCardFavoriteRepository extends JpaRepository<TrainingCardFavorite, Long> {

    boolean existsByCard_IdAndUser_Id(Long cardId, Long userId);

    Optional<TrainingCardFavorite> findByCard_IdAndUser_Id(Long cardId, Long userId);

    @Query("""
            select favorite.card.id
            from TrainingCardFavorite favorite
            where favorite.user.id = :userId
              and favorite.card.id in :cardIds
            """)
    List<Long> findFavoritedCardIdsByUserIdAndCardIds(
            @Param("userId") Long userId,
            @Param("cardIds") Collection<Long> cardIds
    );
}
