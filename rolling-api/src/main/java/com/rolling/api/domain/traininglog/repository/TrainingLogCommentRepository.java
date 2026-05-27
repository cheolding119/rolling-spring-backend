package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingLogComment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TrainingLogCommentRepository extends JpaRepository<TrainingLogComment, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteAllByEntry_Id(Long entryId);

    @EntityGraph(attributePaths = {"author", "parentComment", "entry"})
    List<TrainingLogComment> findAllByEntry_IdOrderByCreatedAtAscIdAsc(Long entryId);

    @EntityGraph(attributePaths = {"author", "parentComment", "parentComment.author", "entry", "entry.user"})
    List<TrainingLogComment> findAllByEntry_IdInOrderByEntry_IdAscCreatedAtAscIdAsc(Collection<Long> entryIds);

    boolean existsByParentComment_Id(Long parentCommentId);

    List<TrainingLogComment> findAllByParentComment_Id(Long parentCommentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"author", "parentComment", "parentComment.author", "entry", "entry.user"})
    @Query("select comment from TrainingLogComment comment where comment.id = :commentId")
    java.util.Optional<TrainingLogComment> findByIdForUpdate(@Param("commentId") Long commentId);

    @Query("""
            select comment.entry.id as entryId, count(comment) as count
            from TrainingLogComment comment
            where comment.entry.id in :entryIds
              and comment.deleted = false
            group by comment.entry.id
            """)
    List<TrainingLogCountProjection> countActiveByEntryIds(@Param("entryIds") Collection<Long> entryIds);
}
