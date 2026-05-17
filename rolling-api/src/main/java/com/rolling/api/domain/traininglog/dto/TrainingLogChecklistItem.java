package com.rolling.api.domain.traininglog.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "훈련 기록 체크리스트 항목")
public record TrainingLogChecklistItem(
        @Schema(description = "체크리스트 항목 내용", example = "트라이앵글 디테일 복습")
        String text,
        @Schema(description = "완료 여부", example = "false")
        boolean checked
) {
}
