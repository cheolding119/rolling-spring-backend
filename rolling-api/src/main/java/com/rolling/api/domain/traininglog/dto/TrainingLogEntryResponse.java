package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogColor;
import com.rolling.api.domain.traininglog.entity.TrainingLogVisibility;
import com.rolling.api.domain.user.entity.BeltColor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
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

    @Schema(description = "기록 색상", example = "BLUE")
    private TrainingLogColor color;

    @Schema(description = "기록 공개 범위", example = "PRIVATE")
    private TrainingLogVisibility visibility;

    @Schema(description = "훈련 강도", example = "3")
    private Integer trainingIntensity;

    @Schema(description = "체육관 출석 여부", example = "true")
    private Boolean gymAttendance;

    @Schema(description = "컨디션", example = "4")
    private Integer condition;

    @Schema(description = "훈련 시간(분)", example = "90")
    private Integer trainingMinutes;

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

    @Schema(description = "이미지 목록")
    private List<String> imageUrls;

    @Schema(description = "대표 이미지 URL", example = "https://cdn.rolling.com/training/logs/images/sample.jpg")
    private String imageUrl;

    @Schema(description = "승급 기록 전용 벨트 색상", example = "BLUE")
    private BeltColor beltColor;

    @Schema(description = "승급 기록 전용 stripe 수", example = "1")
    private Integer stripeCount;

    @Schema(description = "좋아요 수", example = "3")
    private Long likeCount;

    @Schema(description = "댓글 수", example = "5")
    private Long commentCount;

    @Schema(description = "현재 로그인 사용자의 좋아요 여부", example = "false")
    private Boolean likedByMe;

    @Schema(description = "현재 로그인 사용자의 댓글 작성 가능 여부", example = "false")
    private Boolean commentableByMe;

}
