package com.rolling.api.domain.community.dto;

import com.rolling.api.domain.community.entity.CommunityPostCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "커뮤니티 게시글 수정 요청")
public class CommunityPostUpdateRequest {

    @Schema(description = "게시글 카테고리", example = "TECHNIQUE_QNA")
    private CommunityPostCategory category;

    @Size(min = 2, max = 80, message = "title은 2자 이상 80자 이하여야 합니다")
    @Schema(description = "게시글 제목", example = "암바 방어가 계속 뚫립니다")
    private String title;

    @Size(min = 10, max = 5000, message = "content는 10자 이상 5000자 이하여야 합니다")
    @Schema(description = "게시글 본문", example = "스파링 때마다 암바를 자주 허용합니다...")
    private String content;

    @Size(max = 5, message = "imageUrls는 최대 5장까지 허용합니다")
    @Schema(description = "첨부 이미지 URL 목록", nullable = true)
    private List<String> imageUrls;
}
