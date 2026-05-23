package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingLogComment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface TrainingLogCommentRepository extends JpaRepository<TrainingLogComment, Long> {

    @EntityGraph(attributePaths = {"author", "parentComment", "entry"})
    List<TrainingLogComment> findAllByEntry_IdOrderByCreatedAtAscIdAsc(Long entryId);

    boolean existsByParentComment_Id(Long parentCommentId);

    @Query("""
            select comment.entry.id as entryId, count(comment) as count
            from TrainingLogComment comment
            where comment.entry.id in :entryIds
              and comment.deleted = false
            group by comment.entry.id
            """)
    List<TrainingLogCountProjection> countActiveByEntryIds(@Param("entryIds") Collection<Long> entryIds);
}
