package com.rolling.api.domain.traininglog.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class TrainingLogCommentUpdateRequest {

    @NotBlank(message = "content는 필수입니다")
    @Size(min = 1, max = 1000, message = "content는 1자 이상 1000자 이하여야 합니다")
    private String content;
}
