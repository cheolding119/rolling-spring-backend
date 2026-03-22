package com.rolling.api.domain.inquiry.dto;

import com.rolling.api.domain.inquiry.entity.InquiryType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "문의 생성 요청")
public class InquiryCreateRequest {

    @NotBlank(message = "문의 제목은 필수입니다")
    @Size(max = 255, message = "문의 제목은 255자 이하여야 합니다")
    @Schema(description = "문의 제목", example = "알림이 오지 않습니다")
    private String title;

    @NotBlank(message = "문의 내용은 필수입니다")
    @Size(max = 5000, message = "문의 내용은 5000자 이하여야 합니다")
    @Schema(description = "문의 본문", example = "오픈매트 수정 알림이 오지 않습니다. 확인 부탁드립니다.")
    private String content;

    @Schema(
            description = "문의 유형. 생략하면 OTHER로 저장됩니다.",
            example = "NOTIFICATION",
            allowableValues = {"ACCOUNT", "OPEN_MAT", "TOURNAMENT", "NOTIFICATION", "REPORT", "OTHER"}
    )
    private InquiryType type;
}
