package com.rolling.api.domain.traininglog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogColor;
import com.rolling.api.domain.user.entity.BeltColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
@Schema(description = "훈련 기록 수정 요청")
public class TrainingLogEntryUpdateRequest {

    @Schema(
            description = "기록 카테고리",
            example = "TECHNIQUE",
            allowableValues = {"TECHNIQUE", "SPARRING", "TOURNAMENT", "PROMOTION", "OPEN_MAT", "DRILL", "PERSONAL_TRAINING"}
    )
    private TrainingLogCategory category;

    @Size(max = 255, message = "기록 제목은 255자 이하여야 합니다")
    @Schema(description = "기록 제목", example = "암 트라이앵글 디테일 정리")
    private String title;

    @Size(max = 5000, message = "기록 내용은 5000자 이하여야 합니다")
    @Schema(description = "기록 내용", example = "상대 팔 각도와 무릎 각도에 따라 마무리 디테일을 정리했다.")
    private String content;

    @Valid
    @Schema(description = "체크리스트. 빈 배열 또는 null을 보내면 비운다.")
    private List<TrainingLogChecklistItemRequest> checklist;

    @JsonIgnore
    private boolean checklistFieldPresent;

    @Schema(description = "해시태그 목록. 빈 배열 또는 null을 보내면 비운다.", example = "[\"triangle\", \"armbar\"]")
    private List<String> hashtags;

    @JsonIgnore
    private boolean hashtagsFieldPresent;

    @Valid
    @Size(max = 3, message = "외부 링크는 최대 3개까지 입력할 수 있습니다")
    @Schema(description = "외부 링크 목록. 빈 배열 또는 null을 보내면 비운다.")
    private List<TrainingLogExternalLinkRequest> externalLinks;

    @JsonIgnore
    private boolean externalLinksFieldPresent;

    @Size(max = 10, message = "이미지는 최대 10장까지 입력할 수 있습니다")
    @Schema(description = "이미지 목록. 빈 배열 또는 null을 보내면 비운다.", example = "[\"https://cdn.rolling.com/training/logs/images/1.jpg\", \"https://cdn.rolling.com/training/logs/images/2.jpg\"]")
    private List<String> imageUrls;

    @JsonIgnore
    private boolean imageUrlsFieldPresent;

    @Size(max = 1000, message = "imageUrl은 1000자 이하여야 합니다")
    @Schema(description = "대표 이미지 URL. 하위 호환용 단일 입력값. null을 보내면 비운다.", example = "https://cdn.rolling.com/training/logs/images/sample.jpg", nullable = true)
    private String imageUrl;

    @JsonIgnore
    private boolean imageUrlFieldPresent;

    @Schema(description = "기록 색상. null을 보내면 비운다.", example = "BLUE", nullable = true)
    private TrainingLogColor color;

    @JsonIgnore
    private boolean colorFieldPresent;

    @Schema(description = "승급 기록 전용 벨트 색상. null을 보내면 비운다.", example = "BLUE", nullable = true)
    private BeltColor beltColor;

    @JsonIgnore
    private boolean beltColorFieldPresent;

    @Min(value = 0, message = "stripeCount는 0 이상이어야 합니다")
    @Schema(description = "승급 기록 전용 stripe 수. null을 보내면 비운다.", example = "1", nullable = true)
    private Integer stripeCount;

    @JsonIgnore
    private boolean stripeCountFieldPresent;

    @JsonSetter("checklist")
    public void setChecklist(List<TrainingLogChecklistItemRequest> checklist) {
        this.checklist = checklist;
        this.checklistFieldPresent = true;
    }

    @JsonSetter("hashtags")
    public void setHashtags(List<String> hashtags) {
        this.hashtags = hashtags;
        this.hashtagsFieldPresent = true;
    }

    @JsonSetter("externalLinks")
    public void setExternalLinks(List<TrainingLogExternalLinkRequest> externalLinks) {
        this.externalLinks = externalLinks;
        this.externalLinksFieldPresent = true;
    }

    @JsonSetter("imageUrls")
    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
        this.imageUrlsFieldPresent = true;
    }

    @JsonSetter("imageUrl")
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
        this.imageUrlFieldPresent = true;
    }

    @JsonSetter("color")
    public void setColor(TrainingLogColor color) {
        this.color = color;
        this.colorFieldPresent = true;
    }

    @JsonSetter("beltColor")
    public void setBeltColor(BeltColor beltColor) {
        this.beltColor = beltColor;
        this.beltColorFieldPresent = true;
    }

    @JsonSetter("stripeCount")
    public void setStripeCount(Integer stripeCount) {
        this.stripeCount = stripeCount;
        this.stripeCountFieldPresent = true;
    }

    @JsonIgnore
    public boolean hasChecklistField() {
        return checklistFieldPresent;
    }

    @JsonIgnore
    public boolean hasHashtagsField() {
        return hashtagsFieldPresent;
    }

    @JsonIgnore
    public boolean hasExternalLinksField() {
        return externalLinksFieldPresent;
    }

    @JsonIgnore
    public boolean hasImageUrlsField() {
        return imageUrlsFieldPresent;
    }

    @JsonIgnore
    public boolean hasImageUrlField() {
        return imageUrlFieldPresent;
    }

    @JsonIgnore
    public boolean hasColorField() {
        return colorFieldPresent;
    }

    @JsonIgnore
    public boolean hasBeltColorField() {
        return beltColorFieldPresent;
    }

    @JsonIgnore
    public boolean hasStripeCountField() {
        return stripeCountFieldPresent;
    }
}
