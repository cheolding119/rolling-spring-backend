package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingLogColor;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "훈련 기록 요약 응답")
public class TrainingLogEntrySummaryResponse {

    @Schema(description = "기록 ID", example = "1")
    private Long id;

    @Schema(description = "기록 제목", example = "암 트라이앵글 디테일 정리")
    private String title;

    @Schema(description = "기록 내용")
    private String content;

    @Schema(description = "기록 카테고리", example = "TECHNIQUE")
    private TrainingLogCategory category;

    @Schema(description = "기록 색상", example = "BLUE")
    private TrainingLogColor color;

    @Schema(description = "좋아요 수", example = "3")
    private Long likeCount;

    @Schema(description = "댓글 수", example = "5")
    private Long commentCount;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;
}
