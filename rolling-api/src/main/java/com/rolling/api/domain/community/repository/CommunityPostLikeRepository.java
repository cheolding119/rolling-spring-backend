package com.rolling.api.domain.community.repository;

import com.rolling.api.domain.community.entity.CommunityPostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommunityPostLikeRepository extends JpaRepository<CommunityPostLike, Long> {

    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);

    Optional<CommunityPostLike> findByPost_IdAndUser_Id(Long postId, Long userId);

    void deleteByPost_IdAndUser_Id(Long postId, Long userId);
}
