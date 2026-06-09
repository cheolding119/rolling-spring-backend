package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingCardLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainingCardLikeRepository extends JpaRepository<TrainingCardLike, Long> {

    boolean existsByCard_IdAndUser_Id(Long cardId, Long userId);

    Optional<TrainingCardLike> findByCard_IdAndUser_Id(Long cardId, Long userId);

    @Query("""
            select like.card.id as entryId, count(like) as count
            from TrainingCardLike like
            where like.card.id in :cardIds
            group by like.card.id
            """)
    List<TrainingLogCountProjection> countByCardIds(@Param("cardIds") Collection<Long> cardIds);

    @Query("""
            select like.card.id
            from TrainingCardLike like
            where like.user.id = :userId
              and like.card.id in :cardIds
            """)
    List<Long> findLikedCardIdsByUserIdAndCardIds(
            @Param("userId") Long userId,
            @Param("cardIds") Collection<Long> cardIds
    );
}
