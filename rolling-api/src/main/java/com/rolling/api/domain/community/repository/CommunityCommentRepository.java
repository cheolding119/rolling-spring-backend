package com.rolling.api.domain.community.repository;

import com.rolling.api.domain.community.entity.CommunityComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunityCommentRepository extends JpaRepository<CommunityComment, Long> {

    @EntityGraph(attributePaths = {"author", "post"})
    @Query(
            value = """
                    select c from CommunityComment c
                    where c.post.id = :postId
                      and c.post.status = com.rolling.api.domain.community.entity.CommunityPostStatus.ACTIVE
                      and c.post.deletedAt is null
                      and c.status = com.rolling.api.domain.community.entity.CommunityCommentStatus.ACTIVE
                      and c.deletedAt is null
                      and (
                            :viewerUserId is null
                            or c.author.id not in (
                                select blocked.blockedUser.id
                                from User viewer
                                join viewer.blockedUserLinks blocked
                                where viewer.id = :viewerUserId
                            )
                      )
                    """,
            countQuery = """
                    select count(c) from CommunityComment c
                    where c.post.id = :postId
                      and c.post.status = com.rolling.api.domain.community.entity.CommunityPostStatus.ACTIVE
                      and c.post.deletedAt is null
                      and c.status = com.rolling.api.domain.community.entity.CommunityCommentStatus.ACTIVE
                      and c.deletedAt is null
                      and (
                            :viewerUserId is null
                            or c.author.id not in (
                                select blocked.blockedUser.id
                                from User viewer
                                join viewer.blockedUserLinks blocked
                                where viewer.id = :viewerUserId
                            )
                      )
                    """
    )
    Page<CommunityComment> findVisibleByPostId(
            @Param("postId") Long postId,
            @Param("viewerUserId") Long viewerUserId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"author", "post"})
    Optional<CommunityComment> findById(Long id);

    @EntityGraph(attributePaths = {"author", "post", "post.author"})
    @Query(
            value = """
                    select c from CommunityComment c
                    where (:postId is null or c.post.id = :postId)
                      and (:status is null or c.status = :status)
                      and (
                            :keyword is null
                            or lower(c.content) like lower(concat('%', :keyword, '%'))
                      )
                    """,
            countQuery = """
                    select count(c) from CommunityComment c
                    where (:postId is null or c.post.id = :postId)
                      and (:status is null or c.status = :status)
                      and (
                            :keyword is null
                            or lower(c.content) like lower(concat('%', :keyword, '%'))
                      )
                    """
    )
    Page<CommunityComment> findAdminComments(
            @Param("postId") Long postId,
            @Param("status") com.rolling.api.domain.community.entity.CommunityCommentStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );
}
