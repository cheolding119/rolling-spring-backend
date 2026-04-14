package com.rolling.api.domain.user.repository;

import com.rolling.api.domain.user.entity.UserBlock;
import com.rolling.api.domain.user.entity.UserBlockId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserBlockRepository extends JpaRepository<UserBlock, UserBlockId> {

    Optional<UserBlock> findByUser_IdAndBlockedUser_Id(Long userId, Long blockedUserId);

    @EntityGraph(attributePaths = "blockedUser")
    List<UserBlock> findAllByUser_IdAndBlockedUser_IsWithdrawnFalseOrderByBlockedAtDesc(Long userId);
}
