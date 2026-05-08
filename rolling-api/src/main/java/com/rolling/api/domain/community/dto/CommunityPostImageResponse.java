package com.rolling.api.domain.community.dto;

import com.rolling.api.domain.community.entity.CommunityPostImage;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "커뮤니티 게시글 이미지 응답")
public class CommunityPostImageResponse {

    @Schema(description = "이미지 ID", example = "1")
    private Long id;

    @Schema(description = "이미지 URL")
    private String imageUrl;

    @Schema(description = "노출 순서", example = "0")
    private Integer sortOrder;

    public static CommunityPostImageResponse from(CommunityPostImage image) {
        return CommunityPostImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .sortOrder(image.getSortOrder())
                .build();
    }
}
