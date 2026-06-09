package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingCard;
import com.rolling.api.domain.traininglog.entity.TrainingCardLevel;
import com.rolling.api.domain.traininglog.entity.TrainingCardPosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Collection;

public interface TrainingCardRepository extends JpaRepository<TrainingCard, Long> {

    @Query("""
            select card
            from TrainingCard card
            where card.active = true
              and (:level is null or card.level = :level)
              and (:position is null or card.position = :position)
              and (
                    :query is null
                    or lower(card.title) like lower(concat('%', :query, '%'))
                    or lower(card.summary) like lower(concat('%', :query, '%'))
                    or lower(card.topic) like lower(concat('%', :query, '%'))
                  )
            order by card.displayOrder asc, card.id asc
            """)
    List<TrainingCard> searchActiveCards(
            @Param("query") String query,
            @Param("level") TrainingCardLevel level,
            @Param("position") TrainingCardPosition position
    );

    Optional<TrainingCard> findByIdAndActiveTrue(Long id);

    List<TrainingCard> findAllByIdInAndActiveTrue(Collection<Long> ids);
}
