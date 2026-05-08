package com.rolling.api.domain.community.dto;

import com.rolling.api.domain.community.entity.CommunityPost;
import com.rolling.api.domain.community.entity.CommunityPostCategory;
import com.rolling.api.domain.community.entity.CommunityPostStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "커뮤니티 관리자 게시글 응답")
public class CommunityAdminPostResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "카테고리", example = "TECHNIQUE_QNA")
    private CommunityPostCategory category;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "본문")
    private String content;

    @Schema(description = "작성자 ID", example = "10")
    private Long authorId;

    @Schema(description = "작성자 닉네임")
    private String authorNickname;

    @Schema(description = "좋아요 수")
    private Long likeCount;

    @Schema(description = "댓글 수")
    private Long commentCount;

    @Schema(description = "신고 수")
    private Long reportCount;

    @Schema(description = "게시글 상태")
    private CommunityPostStatus status;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;

    @Schema(description = "삭제 시각")
    private LocalDateTime deletedAt;

    public static CommunityAdminPostResponse from(CommunityPost post) {
        return CommunityAdminPostResponse.builder()
                .id(post.getId())
                .category(post.getCategory())
                .title(post.getTitle())
                .content(post.getContent())
                .authorId(post.getAuthor().getId())
                .authorNickname(post.getAuthor().getCommunityNickname())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .reportCount(post.getReportCount())
                .status(post.getStatus())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .deletedAt(post.getDeletedAt())
                .build();
    }
}
