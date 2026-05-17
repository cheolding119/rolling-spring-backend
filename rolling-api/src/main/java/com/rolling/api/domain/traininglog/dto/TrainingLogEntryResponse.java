package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.user.entity.BeltColor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@Schema(description = "훈련 기록 응답")
public class TrainingLogEntryResponse {

    @Schema(description = "기록 ID", example = "1")
    private Long id;

    @Schema(description = "훈련 날짜", example = "2026-05-17")
    private LocalDate trainingDate;

    @Schema(description = "기록 카테고리", example = "TECHNIQUE")
    private TrainingLogCategory category;

    @Schema(description = "기록 제목", example = "암 트라이앵글 디테일 정리")
    private String title;

    @Schema(description = "기록 내용")
    private String content;

    @Schema(description = "체크리스트")
    private List<TrainingLogChecklistItem> checklist;

    @Schema(description = "해시태그 목록")
    private List<String> hashtags;

    @Schema(description = "외부 링크 목록")
    private List<TrainingLogExternalLink> externalLinks;

    @Schema(description = "대표 이미지 URL", example = "https://cdn.rolling.com/training/logs/images/sample.jpg")
    private String imageUrl;

    @Schema(description = "훈련 시간(분)", example = "90")
    private Integer trainingMinutes;

    @Schema(description = "승급 기록 전용 벨트 색상", example = "BLUE")
    private BeltColor beltColor;

    @Schema(description = "승급 기록 전용 stripe 수", example = "1")
    private Integer stripeCount;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;
}
