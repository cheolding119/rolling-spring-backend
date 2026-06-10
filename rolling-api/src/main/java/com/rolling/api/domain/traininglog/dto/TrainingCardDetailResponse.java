package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingCard;
import com.rolling.api.domain.traininglog.entity.TrainingCardLevel;
import com.rolling.api.domain.traininglog.entity.TrainingCardPosition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class TrainingCardDetailResponse {

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

    @Schema(description = "기술 상세 설명")
    private String description;

    @Schema(description = "기술이 쓰이는 상황에 대한 설명")
    private String situationDescription;

    @Schema(description = "시작 자세 또는 전제 상황 설명")
    private String startingPositionDescription;

    @Schema(description = "단계형 기술 흐름 설명")
    private String flowDescription;

    @Schema(description = "핵심 포인트 설명")
    private String keyPoints;

    @Schema(description = "자주 틀리는 점 설명")
    private String commonMistakes;

    @Schema(description = "주의할 점 설명")
    private String cautions;

    @Schema(description = "유튜브 링크", example = "https://www.youtube.com/watch?v=example")
    private String youtubeUrl;

    @Schema(description = "좋아요 수", example = "12")
    private Long likeCount;

    @Schema(description = "현재 로그인 사용자의 좋아요 여부", example = "false")
    private Boolean likedByMe;

    @Schema(description = "현재 로그인 사용자의 즐겨찾기 여부", example = "false")
    private Boolean favoritedByMe;

    @Schema(description = "연관 훈련카드 목록")
    private List<TrainingCardRelatedItemResponse> relatedCards;

    public static TrainingCardDetailResponse from(
            TrainingCard card,
            long likeCount,
            boolean likedByMe,
            boolean favoritedByMe,
            List<TrainingCardRelatedItemResponse> relatedCards
    ) {
        return TrainingCardDetailResponse.builder()
                .id(card.getId())
                .title(card.getTitle())
                .summary(card.getSummary())
                .topic(card.getTopic())
                .level(card.getLevel())
                .position(card.getPosition())
                .situationSummary(card.getSituationSummary())
                .description(card.getDescription())
                .situationDescription(card.getSituationDescription())
                .startingPositionDescription(card.getStartingPositionDescription())
                .flowDescription(card.getFlowDescription())
                .keyPoints(card.getKeyPoints())
                .commonMistakes(card.getCommonMistakes())
                .cautions(card.getCautions())
                .youtubeUrl(card.getYoutubeUrl())
                .likeCount(likeCount)
                .likedByMe(likedByMe)
                .favoritedByMe(favoritedByMe)
                .relatedCards(relatedCards)
                .build();
    }
}
