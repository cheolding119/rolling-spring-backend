package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingLogEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TrainingLogEntryRepository extends JpaRepository<TrainingLogEntry, Long> {

    List<TrainingLogEntry> findAllByUser_IdAndTrainingDateOrderByCreatedAtAsc(Long userId, LocalDate trainingDate);

    List<TrainingLogEntry> findAllByUser_IdOrderByTrainingDateDescCreatedAtDesc(Long userId, Pageable pageable);

    List<TrainingLogEntry> findAllByUser_IdAndTrainingDateGreaterThanEqualAndTrainingDateLessThanOrderByTrainingDateAscCreatedAtAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDateExclusive
    );

    @EntityGraph(attributePaths = "user")
    Optional<TrainingLogEntry> findWithUserById(Long id);

    Optional<TrainingLogEntry> findFirstByUser_IdAndCategoryOrderByTrainingDateDescCreatedAtDescIdDesc(
            Long userId,
            com.rolling.api.domain.traininglog.entity.TrainingLogCategory category
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

    @EntityGraph(attributePaths = "user")
    @Query(
            value = """
                    select entry
                    from TrainingLogEntry entry
                    where entry.user.id in (
                          select friendship.friendUser.id
                          from Friendship friendship
                          where friendship.user.id = :viewerUserId
                      )
                      and exists (
                          select 1
                          from UserTrainingLogShareSetting shareSetting
                          where shareSetting.user.id = entry.user.id
                            and shareSetting.shareWithFriends = true
                      )
                      and entry.user.isWithdrawn = false
                      and entry.user.withdrawalPending = false
                      and entry.user.accountStatus = com.rolling.api.domain.user.entity.AccountStatus.ACTIVE
                      and not exists (
                          select 1 from UserBlock block
                          where (block.user.id = :viewerUserId and block.blockedUser.id = entry.user.id)
                             or (block.user.id = entry.user.id and block.blockedUser.id = :viewerUserId)
                      )
                    """,
            countQuery = """
                    select count(entry)
                    from TrainingLogEntry entry
                    where entry.user.id in (
                          select friendship.friendUser.id
                          from Friendship friendship
                          where friendship.user.id = :viewerUserId
                      )
                      and exists (
                          select 1
                          from UserTrainingLogShareSetting shareSetting
                          where shareSetting.user.id = entry.user.id
                            and shareSetting.shareWithFriends = true
                      )
                      and entry.user.isWithdrawn = false
                      and entry.user.withdrawalPending = false
                      and entry.user.accountStatus = com.rolling.api.domain.user.entity.AccountStatus.ACTIVE
                      and not exists (
                          select 1 from UserBlock block
                          where (block.user.id = :viewerUserId and block.blockedUser.id = entry.user.id)
                             or (block.user.id = entry.user.id and block.blockedUser.id = :viewerUserId)
                      )
                    """
    )
    Page<TrainingLogEntry> findFriendFeedEntries(
            @Param("viewerUserId") Long viewerUserId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = "user")
    List<TrainingLogEntry> findAllByUser_IdAndTrainingDateOrderByCreatedAtAscIdAsc(Long userId, LocalDate trainingDate);
}
