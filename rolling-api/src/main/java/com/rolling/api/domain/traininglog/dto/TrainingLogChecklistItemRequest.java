package com.rolling.api.domain.traininglog.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "훈련 기록 체크리스트 항목 요청")
public class TrainingLogChecklistItemRequest {

    @NotBlank(message = "체크리스트 항목 내용은 필수입니다")
    @Size(max = 255, message = "체크리스트 항목 내용은 255자 이하여야 합니다")
    @Schema(description = "체크리스트 항목 내용", example = "트라이앵글 디테일 복습")
    private String text;

    @Schema(description = "완료 여부", example = "false")
    private Boolean checked;
}
