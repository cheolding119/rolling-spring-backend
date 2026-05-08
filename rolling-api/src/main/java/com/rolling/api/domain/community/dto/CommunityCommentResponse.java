package com.rolling.api.domain.community.dto;

import com.rolling.api.domain.community.entity.CommunityComment;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "커뮤니티 댓글 응답")
public class CommunityCommentResponse {

    @Schema(description = "댓글 ID", example = "1")
    private Long id;

    @Schema(description = "게시글 ID", example = "1")
    private Long postId;

    @Schema(description = "작성자 닉네임", example = "노기좋아하는거북이")
    private String authorNickname;

    @Schema(description = "댓글 본문")
    private String content;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;

    public static CommunityCommentResponse from(CommunityComment comment) {
        return CommunityCommentResponse.builder()
                .id(comment.getId())
                .postId(comment.getPost().getId())
                .authorNickname(comment.getAuthor().getCommunityNickname())
                .content(comment.getContent())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
