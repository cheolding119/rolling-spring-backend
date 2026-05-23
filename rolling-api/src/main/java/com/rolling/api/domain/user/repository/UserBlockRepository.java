package com.rolling.api.domain.user.repository;

import com.rolling.api.domain.user.entity.UserBlock;
import com.rolling.api.domain.user.entity.UserBlockId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlockId> {

    boolean existsByUser_IdAndBlockedUser_Id(Long userId, Long blockedUserId);

    Optional<UserBlock> findByUser_IdAndBlockedUser_Id(Long userId, Long blockedUserId);

    @EntityGraph(attributePaths = "blockedUser")
    List<UserBlock> findAllByUser_IdAndBlockedUser_IsWithdrawnFalseOrderByBlockedAtDesc(Long userId);

    @Query("""
            select distinct case
                when block.user.id = :userId then block.blockedUser.id
                else block.user.id
            end
            from UserBlock block
            where (block.user.id = :userId and block.blockedUser.id in :candidateIds)
               or (block.blockedUser.id = :userId and block.user.id in :candidateIds)
            """)
    List<Long> findBlockedRelationUserIds(
            @Param("userId") Long userId,
            @Param("candidateIds") Collection<Long> candidateIds
    );
}
