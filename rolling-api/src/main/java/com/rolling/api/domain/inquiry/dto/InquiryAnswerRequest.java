package com.rolling.api.domain.inquiry.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "문의 답변 저장 요청")
public class InquiryAnswerRequest {

    @NotBlank(message = "문의 답변은 필수입니다")
    @Size(max = 5000, message = "문의 답변은 5000자 이하여야 합니다")
    @Schema(description = "운영자 답변", example = "알림 권한과 FCM 토큰 상태를 확인했고, 현재 기기에서 다시 등록되도록 수정했습니다.")
    private String answerContent;
}
