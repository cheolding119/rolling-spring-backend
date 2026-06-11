package com.rolling.api.domain.openmat.repository;

import com.rolling.api.domain.openmat.entity.OpenMatComment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OpenMatCommentRepository extends JpaRepository<OpenMatComment, Long> {

    @EntityGraph(attributePaths = {"author", "parentComment", "parentComment.author", "openMat", "openMat.host"})
    List<OpenMatComment> findAllByOpenMat_IdOrderByCreatedAtAscIdAsc(Long openMatId);

    List<OpenMatComment> findAllByParentComment_Id(Long parentCommentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"author", "parentComment", "parentComment.author", "openMat", "openMat.host"})
    @Query("select comment from OpenMatComment comment where comment.id = :commentId")
    Optional<OpenMatComment> findByIdForUpdate(@Param("commentId") Long commentId);
}
