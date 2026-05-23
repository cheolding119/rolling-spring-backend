package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.Friendship;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    boolean existsByUser_IdAndFriendUser_Id(Long userId, Long friendUserId);

    @EntityGraph(attributePaths = "friendUser")
    List<Friendship> findAllByUser_IdOrderByFriendedAtDesc(Long userId);

    @Query("""
            select friendship.friendUser.id
            from Friendship friendship
            where friendship.user.id = :userId
            """)
    List<Long> findFriendUserIdsByUserId(@Param("userId") Long userId);

    void deleteAllByUser_IdAndFriendUser_Id(Long userId, Long friendUserId);
}
