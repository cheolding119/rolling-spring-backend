package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingLogEntryCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TrainingLogEntryCardRepository extends JpaRepository<TrainingLogEntryCard, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteAllByEntry_Id(Long entryId);

    @Query("""
            select entryCard
            from TrainingLogEntryCard entryCard
            join fetch entryCard.card card
            where entryCard.entry.id in :entryIds
            order by entryCard.entry.id asc, entryCard.id asc
            """)
    List<TrainingLogEntryCard> findAllByEntryIdsWithCard(@Param("entryIds") Collection<Long> entryIds);
}
