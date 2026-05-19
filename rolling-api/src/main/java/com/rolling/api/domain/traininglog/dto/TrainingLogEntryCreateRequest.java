package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogColor;
import com.rolling.api.domain.user.entity.BeltColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "훈련 기록 생성 요청")
public class TrainingLogEntryCreateRequest {

    @NotNull(message = "기록 카테고리는 필수입니다")
    @Schema(
            description = "기록 카테고리",
            example = "TECHNIQUE",
            allowableValues = {"TECHNIQUE", "SPARRING", "TOURNAMENT", "PROMOTION", "OPEN_MAT", "DRILL", "PERSONAL_TRAINING"}
    )
    private TrainingLogCategory category;

    @NotBlank(message = "기록 제목은 필수입니다")
    @Size(max = 255, message = "기록 제목은 255자 이하여야 합니다")
    @Schema(description = "기록 제목", example = "암 트라이앵글 디테일 정리")
    private String title;

    @NotBlank(message = "기록 내용은 필수입니다")
    @Size(max = 5000, message = "기록 내용은 5000자 이하여야 합니다")
    @Schema(description = "기록 내용", example = "상대 팔 각도와 무릎 각도에 따라 마무리 디테일을 정리했다.")
    private String content;

    @Valid
    @Schema(description = "체크리스트")
    private List<TrainingLogChecklistItemRequest> checklist;

    @Schema(description = "해시태그 목록", example = "[\"triangle\", \"armbar\"]")
    private List<String> hashtags;

    @Valid
    @Size(max = 3, message = "외부 링크는 최대 3개까지 입력할 수 있습니다")
    @Schema(description = "외부 링크 목록")
    private List<TrainingLogExternalLinkRequest> externalLinks;

    @Size(max = 10, message = "이미지는 최대 10장까지 입력할 수 있습니다")
    @Schema(description = "이미지 목록", example = "[\"https://cdn.rolling.com/training/logs/images/1.jpg\", \"https://cdn.rolling.com/training/logs/images/2.jpg\"]")
    private List<String> imageUrls;

    @Size(max = 1000, message = "imageUrl은 1000자 이하여야 합니다")
    @Schema(description = "대표 이미지 URL. 하위 호환용 단일 이미지 입력값", example = "https://cdn.rolling.com/training/logs/images/sample.jpg")
    private String imageUrl;

    @Schema(description = "기록 색상", example = "BLUE")
    private TrainingLogColor color;

    @Min(value = 1, message = "훈련 강도는 1 이상이어야 합니다")
    @Max(value = 5, message = "훈련 강도는 5 이하이어야 합니다")
    @Schema(description = "훈련 강도", example = "3")
    private Integer trainingIntensity;

    @Schema(description = "승급 기록 전용 벨트 색상", example = "BLUE")
    private BeltColor beltColor;

    @Min(value = 0, message = "stripeCount는 0 이상이어야 합니다")
    @Schema(description = "승급 기록 전용 stripe 수", example = "1")
    private Integer stripeCount;
}
