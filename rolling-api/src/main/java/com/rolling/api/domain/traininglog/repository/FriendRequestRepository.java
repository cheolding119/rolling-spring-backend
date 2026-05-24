package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.FriendRequest;
import com.rolling.api.domain.traininglog.entity.FriendRequestStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FriendRequestRepository extends JpaRepository<FriendRequest, Long> {

    @EntityGraph(attributePaths = {"sender", "receiver"})
    List<FriendRequest> findAllByReceiver_IdAndStatusOrderByCreatedAtDesc(Long receiverUserId, FriendRequestStatus status);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    List<FriendRequest> findAllBySender_IdAndStatusOrderByCreatedAtDesc(Long senderUserId, FriendRequestStatus status);

    @EntityGraph(attributePaths = {"sender", "receiver"})
    @Query("""
            select request
            from FriendRequest request
            where request.status = com.rolling.api.domain.traininglog.entity.FriendRequestStatus.PENDING
              and (
                    (request.sender.id = :userId and request.receiver.id in :candidateIds)
                 or (request.receiver.id = :userId and request.sender.id in :candidateIds)
              )
            """)
    List<FriendRequest> findPendingRequestsBetweenUserAndCandidates(
            @Param("userId") Long userId,
            @Param("candidateIds") List<Long> candidateIds
    );

    @Query("""
            select case when count(request) > 0 then true else false end
            from FriendRequest request
            where request.status = :status
              and ((request.sender.id = :firstUserId and request.receiver.id = :secondUserId)
                or (request.sender.id = :secondUserId and request.receiver.id = :firstUserId))
            """)
    boolean existsBetweenUsersWithStatus(
            @Param("firstUserId") Long firstUserId,
            @Param("secondUserId") Long secondUserId,
            @Param("status") FriendRequestStatus status
    );

    @Query("""
            select distinct case
                when request.sender.id = :userId then request.receiver.id
                else request.sender.id
            end
            from FriendRequest request
            where request.status = com.rolling.api.domain.traininglog.entity.FriendRequestStatus.PENDING
              and (request.sender.id = :userId or request.receiver.id = :userId)
            """)
    List<Long> findPendingRelatedUserIds(@Param("userId") Long userId);
}
