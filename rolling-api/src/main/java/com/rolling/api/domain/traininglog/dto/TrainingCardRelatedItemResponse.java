package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingCard;
import com.rolling.api.domain.traininglog.entity.TrainingCardLevel;
import com.rolling.api.domain.traininglog.entity.TrainingCardPosition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TrainingCardRelatedItemResponse {

    @Schema(description = "연관 훈련카드 ID", example = "2")
    private Long id;

    @Schema(description = "기술 제목", example = "Toreando Pass")
    private String title;

    @Schema(description = "짧은 요약", example = "양쪽 다리를 밀어내며 각도를 만들어 통과하는 패스")
    private String summary;

    @Schema(description = "기술 분류", example = "PASS")
    private String topic;

    @Schema(description = "기술 레벨", example = "INTERMEDIATE")
    private TrainingCardLevel level;

    @Schema(description = "기술 포지션", example = "STANDING")
    private TrainingCardPosition position;

    @Schema(description = "어떤 상황에서 쓰는지에 대한 짧은 문구", example = "상대 양발 컨트롤이 가능한 오픈가드 상황")
    private String situationSummary;

    public static TrainingCardRelatedItemResponse from(TrainingCard card) {
        return TrainingCardRelatedItemResponse.builder()
                .id(card.getId())
                .title(card.getTitle())
                .summary(card.getSummary())
                .topic(card.getTopic())
                .level(card.getLevel())
                .position(card.getPosition())
                .situationSummary(card.getSituationSummary())
                .build();
    }
}
