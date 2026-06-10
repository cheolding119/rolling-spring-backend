package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingCardRelation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TrainingCardRelationRepository extends JpaRepository<TrainingCardRelation, Long> {

    @Query("""
            select relation
            from TrainingCardRelation relation
            join fetch relation.relatedCard relatedCard
            where relation.card.id = :cardId
              and relatedCard.active = true
              and relatedCard.id <> :cardId
            order by relation.displayOrder asc, relation.id asc
            """)
    List<TrainingCardRelation> findActiveRelationsByCardId(@Param("cardId") Long cardId);
}
