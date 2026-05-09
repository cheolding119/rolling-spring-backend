package com.rolling.api.domain.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "커뮤니티 게시글 이미지 업로드 URL 발급 응답")
public class CommunityPostImageUploadUrlResponse {

    @Schema(description = "S3 업로드용 presigned URL")
    private String uploadUrl;

    @Schema(description = "서버 저장용 S3 object key")
    private String imageKey;

    @Schema(description = "업로드 후 접근 가능한 공개 이미지 URL")
    private String imageUrl;

    @Schema(description = "URL 만료 시각")
    private LocalDateTime expiresAt;
}
