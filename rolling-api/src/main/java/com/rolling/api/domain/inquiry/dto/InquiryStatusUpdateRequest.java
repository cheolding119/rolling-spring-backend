package com.rolling.api.domain.inquiry.dto;

import com.rolling.api.domain.inquiry.entity.InquiryStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "문의 상태 변경 요청")
public class InquiryStatusUpdateRequest {

    @NotNull(message = "status는 필수입니다")
    @Schema(
            description = "문의 상태",
            example = "IN_REVIEW",
            allowableValues = {"RECEIVED", "IN_REVIEW", "ANSWERED"}
    )
    private InquiryStatus status;
}
