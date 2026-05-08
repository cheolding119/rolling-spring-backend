package com.rolling.api.domain.community.repository;

import com.rolling.api.domain.community.entity.CommunityPost;
import com.rolling.api.domain.community.entity.CommunityPostCategory;
import com.rolling.api.domain.community.entity.CommunityPostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, Long> {

    @EntityGraph(attributePaths = "author")
    @Query(
            value = """
                    select p from CommunityPost p
                    where p.status = com.rolling.api.domain.community.entity.CommunityPostStatus.ACTIVE
                      and (
                            :viewerUserId is null
                            or p.author.id not in (
                                select blocked.blockedUser.id
                                from User viewer
                                join viewer.blockedUserLinks blocked
                                where viewer.id = :viewerUserId
                            )
                      )
                      and (:category is null or p.category = :category)
                      and (
                            :keyword is null
                            or lower(p.title) like lower(concat('%', :keyword, '%'))
                            or lower(p.content) like lower(concat('%', :keyword, '%'))
                      )
                    """,
            countQuery = """
                    select count(p) from CommunityPost p
                    where p.status = com.rolling.api.domain.community.entity.CommunityPostStatus.ACTIVE
                      and (
                            :viewerUserId is null
                            or p.author.id not in (
                                select blocked.blockedUser.id
                                from User viewer
                                join viewer.blockedUserLinks blocked
                                where viewer.id = :viewerUserId
                            )
                      )
                      and (:category is null or p.category = :category)
                      and (
                            :keyword is null
                            or lower(p.title) like lower(concat('%', :keyword, '%'))
                            or lower(p.content) like lower(concat('%', :keyword, '%'))
                      )
                    """
    )
    Page<CommunityPost> searchVisible(
            @Param("viewerUserId") Long viewerUserId,
            @Param("category") CommunityPostCategory category,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"author", "images"})
    @Query("""
            select p from CommunityPost p
            where p.id = :id
              and p.status = com.rolling.api.domain.community.entity.CommunityPostStatus.ACTIVE
              and (
                    :viewerUserId is null
                    or p.author.id not in (
                        select blocked.blockedUser.id
                        from User viewer
                        join viewer.blockedUserLinks blocked
                        where viewer.id = :viewerUserId
                    )
              )
            """)
    Optional<CommunityPost> findVisibleById(@Param("id") Long id, @Param("viewerUserId") Long viewerUserId);
}
