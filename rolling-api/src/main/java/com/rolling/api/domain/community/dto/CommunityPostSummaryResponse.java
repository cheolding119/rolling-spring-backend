package com.rolling.api.domain.community.dto;

import com.rolling.api.domain.community.entity.CommunityPost;
import com.rolling.api.domain.community.entity.CommunityPostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "커뮤니티 게시글 요약 응답")
public class CommunityPostSummaryResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "카테고리", example = "TECHNIQUE_QNA")
    private CommunityPostCategory category;

    @Schema(description = "제목", example = "암바 방어가 계속 뚫립니다")
    private String title;

    @Schema(description = "작성자 닉네임", example = "노기좋아하는거북이")
    private String authorNickname;

    @Schema(description = "대표 이미지 URL", nullable = true)
    private String thumbnailUrl;

    @Schema(description = "좋아요 수", example = "3")
    private Long likeCount;

    @Schema(description = "댓글 수", example = "5")
    private Long commentCount;

    @Schema(description = "조회 수", example = "27")
    private Long viewCount;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    public static CommunityPostSummaryResponse from(CommunityPost post) {
        return CommunityPostSummaryResponse.builder()
                .id(post.getId())
                .category(post.getCategory())
                .title(post.getTitle())
                .authorNickname(post.getAuthor().getCommunityNickname())
                .thumbnailUrl(post.getThumbnailUrl())
                .likeCount(post.getLikeCount())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
