package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingLogLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TrainingLogLikeRepository extends JpaRepository<TrainingLogLike, Long> {

    boolean existsByEntry_IdAndUser_Id(Long entryId, Long userId);

    Optional<TrainingLogLike> findByEntry_IdAndUser_Id(Long entryId, Long userId);

    @Query("""
            select like.entry.id as entryId, count(like) as count
            from TrainingLogLike like
            where like.entry.id in :entryIds
            group by like.entry.id
            """)
    List<TrainingLogCountProjection> countByEntryIds(@Param("entryIds") Collection<Long> entryIds);

    @Query("""
            select like.entry.id
            from TrainingLogLike like
            where like.user.id = :userId
              and like.entry.id in :entryIds
            """)
    List<Long> findLikedEntryIdsByUserIdAndEntryIds(
            @Param("userId") Long userId,
            @Param("entryIds") Collection<Long> entryIds
    );
}
