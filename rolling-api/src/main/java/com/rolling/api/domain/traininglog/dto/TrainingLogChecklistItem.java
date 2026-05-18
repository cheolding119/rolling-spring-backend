package com.rolling.api.domain.traininglog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "훈련 기록 체크리스트 항목")
public record TrainingLogChecklistItem(
        @Schema(description = "체크리스트 항목 내용", example = "트라이앵글 디테일 복습")
        String text,
        @Schema(description = "완료 여부", example = "false")
        boolean checked,
        @Schema(description = "즐겨찾기 여부", example = "false")
        boolean favorite,
        @Schema(description = "즐겨찾기 표시 이모지", example = "🔥", nullable = true)
        String emoji
) {
}
