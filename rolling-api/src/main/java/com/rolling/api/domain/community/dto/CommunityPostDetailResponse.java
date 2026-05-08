package com.rolling.api.domain.community.dto;

import com.rolling.api.domain.community.entity.CommunityPost;
import com.rolling.api.domain.community.entity.CommunityPostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "커뮤니티 게시글 상세 응답")
public class CommunityPostDetailResponse {

    @Schema(description = "게시글 ID", example = "1")
    private Long id;

    @Schema(description = "카테고리", example = "TECHNIQUE_QNA")
    private CommunityPostCategory category;

    @Schema(description = "제목")
    private String title;

    @Schema(description = "본문")
    private String content;

    @Schema(description = "작성자 닉네임")
    private String authorNickname;

    @Schema(description = "대표 이미지 URL", nullable = true)
    private String thumbnailUrl;

    @Schema(description = "댓글 수")
    private Long commentCount;

    @Schema(description = "조회 수")
    private Long viewCount;

    @Schema(description = "좋아요 수")
    private Long likeCount;

    @Schema(description = "로그인 사용자가 좋아요했는지 여부")
    private Boolean likedByMe;

    @Schema(description = "로그인 사용자가 수정/삭제 가능한지 여부")
    private Boolean editableByMe;

    @Schema(description = "이미지 목록")
    private List<CommunityPostImageResponse> images;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;

    public static CommunityPostDetailResponse from(CommunityPost post,
                                                   boolean likedByMe,
                                                   boolean editableByMe,
                                                   List<CommunityPostImageResponse> images) {
        return CommunityPostDetailResponse.builder()
                .id(post.getId())
                .category(post.getCategory())
                .title(post.getTitle())
                .content(post.getContent())
                .authorNickname(post.getAuthor().getCommunityNickname())
                .thumbnailUrl(post.getThumbnailUrl())
                .commentCount(post.getCommentCount())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .likedByMe(likedByMe)
                .editableByMe(editableByMe)
                .images(images)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
