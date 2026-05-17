package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainingLogEntryRepository extends JpaRepository<TrainingLogEntry, Long> {

    List<TrainingLogEntry> findAllByUser_IdAndTrainingDateOrderByCreatedAtAsc(Long userId, LocalDate trainingDate);

    List<TrainingLogEntry> findAllByUser_IdOrderByTrainingDateDescCreatedAtDesc(Long userId, Pageable pageable);

    Optional<TrainingLogEntry> findFirstByUser_IdAndCategoryOrderByTrainingDateDescCreatedAtDescIdDesc(
            Long userId,
            TrainingLogCategory category
    );

    @Query("""
            select new com.rolling.api.domain.traininglog.repository.TrainingLogCalendarDailyProjection(
                entry.trainingDate,
                coalesce(sum(entry.trainingMinutes), 0),
                count(entry)
            )
            from TrainingLogEntry entry
            where entry.user.id = :userId
              and entry.trainingDate >= :startDate
              and entry.trainingDate < :endDateExclusive
            group by entry.trainingDate
            order by entry.trainingDate asc
            """)
    List<TrainingLogCalendarDailyProjection> findDailySummariesByUserIdAndTrainingDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDateExclusive") LocalDate endDateExclusive
    );

    @Query("""
            select new com.rolling.api.domain.traininglog.repository.TrainingLogCalendarMonthlyProjection(
                month(entry.trainingDate),
                coalesce(sum(entry.trainingMinutes), 0),
                count(distinct entry.trainingDate)
            )
            from TrainingLogEntry entry
            where entry.user.id = :userId
              and entry.trainingDate >= :startDate
              and entry.trainingDate < :endDateExclusive
            group by month(entry.trainingDate)
            order by month(entry.trainingDate) asc
            """)
    List<TrainingLogCalendarMonthlyProjection> findMonthlySummariesByUserIdAndTrainingDateBetween(
            @Param("userId") Long userId,
            @Param("startDate") LocalDate startDate,
            @Param("endDateExclusive") LocalDate endDateExclusive
    );

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
