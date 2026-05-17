package com.rolling.api.domain.traininglog.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
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

    @Min(value = 0, message = "훈련 시간은 0 이상이어야 합니다")
    @Max(value = 600, message = "훈련 시간은 600 이하여야 합니다")
    @Schema(description = "훈련 시간(분). null을 보내면 비운다.", example = "90", nullable = true)
    private Integer trainingMinutes;

    @JsonIgnore
    private boolean trainingMinutesFieldPresent;

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

    @JsonSetter("trainingMinutes")
    public void setTrainingMinutes(Integer trainingMinutes) {
        this.trainingMinutes = trainingMinutes;
        this.trainingMinutesFieldPresent = true;
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
    public boolean hasTrainingMinutesField() {
        return trainingMinutesFieldPresent;
    }
}
