package com.rolling.api.domain.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "커뮤니티 댓글 작성 요청")
public class CommunityCommentCreateRequest {

    @NotBlank(message = "content는 필수입니다")
    @Size(min = 1, max = 1000, message = "content는 1자 이상 1000자 이하여야 합니다")
    @Schema(description = "댓글 본문", example = "상체를 먼저 고정해보세요")
    private String content;
}
