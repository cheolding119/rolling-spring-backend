package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface TrainingLogEntryRepository extends JpaRepository<TrainingLogEntry, Long> {

    List<TrainingLogEntry> findAllByUser_IdAndTrainingDateOrderByCreatedAtAsc(Long userId, LocalDate trainingDate);

    @Query("""
            select entry.hashtagsJson
            from TrainingLogEntry entry
            where entry.user.id = :userId
              and entry.hashtagsJson is not null
              and entry.hashtagsJson <> ''
            order by entry.createdAt desc
            """)
    List<String> findHashtagsJsonByUserId(@Param("userId") Long userId);
}
