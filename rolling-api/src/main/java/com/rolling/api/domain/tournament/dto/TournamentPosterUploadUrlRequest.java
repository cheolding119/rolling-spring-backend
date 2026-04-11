package com.rolling.api.domain.tournament.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "대회 포스터 업로드 URL 발급 요청")
public class TournamentPosterUploadUrlRequest {

    @NotBlank(message = "fileName은 필수입니다")
    @Schema(description = "원본 파일명", example = "poster.jpg")
    private String fileName;

    @NotBlank(message = "contentType은 필수입니다")
    @Schema(description = "파일 content type", example = "image/jpeg")
    private String contentType;
}
