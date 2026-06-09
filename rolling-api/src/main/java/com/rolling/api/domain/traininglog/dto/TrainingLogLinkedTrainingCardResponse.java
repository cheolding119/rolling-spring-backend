package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingCard;
import com.rolling.api.domain.traininglog.entity.TrainingCardLevel;
import com.rolling.api.domain.traininglog.entity.TrainingCardPosition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "훈련일지에 연결된 훈련카드 요약 정보")
public class TrainingLogLinkedTrainingCardResponse {

    @Schema(description = "훈련카드 ID", example = "1")
    private Long id;

    @Schema(description = "기술 제목", example = "Knee Cut Pass")
    private String title;

    @Schema(description = "짧은 요약", example = "상대 가드를 가로질러 압박으로 통과하는 패스")
    private String summary;

    @Schema(description = "기술 분류", example = "PASS")
    private String topic;

    @Schema(description = "기술 레벨", example = "BEGINNER")
    private TrainingCardLevel level;

    @Schema(description = "기술 포지션", example = "GUARD")
    private TrainingCardPosition position;

    @Schema(description = "어떤 상황에서 쓰는지에 대한 짧은 문구", example = "상대의 니쉴드나 하프가드 압박 상황")
    private String situationSummary;

    public static TrainingLogLinkedTrainingCardResponse from(TrainingCard card) {
        return TrainingLogLinkedTrainingCardResponse.builder()
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
