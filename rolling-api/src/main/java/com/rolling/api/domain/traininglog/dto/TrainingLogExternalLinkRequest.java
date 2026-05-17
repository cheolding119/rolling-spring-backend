package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingLogLinkType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "훈련 기록 외부 링크 요청")
public class TrainingLogExternalLinkRequest {

    @NotNull(message = "외부 링크 타입은 필수입니다")
    @Schema(description = "링크 타입", example = "INSTAGRAM")
    private TrainingLogLinkType type;

    @NotBlank(message = "외부 링크 URL은 필수입니다")
    @Size(max = 1000, message = "외부 링크 URL은 1000자 이하여야 합니다")
    @Schema(description = "링크 URL", example = "https://www.instagram.com/p/xxxx/")
    private String url;
}
