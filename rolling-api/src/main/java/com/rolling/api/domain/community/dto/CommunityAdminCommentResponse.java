package com.rolling.api.domain.community.dto;

import com.rolling.api.domain.community.entity.CommunityComment;
import com.rolling.api.domain.community.entity.CommunityCommentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "커뮤니티 관리자 댓글 응답")
public class CommunityAdminCommentResponse {

    @Schema(description = "댓글 ID", example = "1")
    private Long id;

    @Schema(description = "게시글 ID", example = "1")
    private Long postId;

    @Schema(description = "게시글 제목")
    private String postTitle;

    @Schema(description = "작성자 ID", example = "10")
    private Long authorId;

    @Schema(description = "작성자 닉네임")
    private String authorNickname;

    @Schema(description = "댓글 본문")
    private String content;

    @Schema(description = "신고 수")
    private Long reportCount;

    @Schema(description = "댓글 상태")
    private CommunityCommentStatus status;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;

    @Schema(description = "삭제 시각")
    private LocalDateTime deletedAt;

    public static CommunityAdminCommentResponse from(CommunityComment comment) {
        return CommunityAdminCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .postTitle(comment.getPost().getTitle())
                .authorId(comment.getAuthor().getId())
                .authorNickname(comment.getAuthor().getCommunityNickname())
                .content(comment.getContent())
                .reportCount(comment.getReportCount())
                .status(comment.getStatus())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .deletedAt(comment.getDeletedAt())
                .build();
    }
}
