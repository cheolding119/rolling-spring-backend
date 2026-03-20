package com.rolling.api.domain.report.dto;

import com.rolling.api.domain.report.entity.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "신고 상태 변경 요청")
public class ReportStatusUpdateRequest {

    @NotNull(message = "status는 필수입니다")
    @Schema(
            description = "신고 처리 상태",
            example = "IN_REVIEW",
            allowableValues = {"RECEIVED", "IN_REVIEW", "RESOLVED", "REJECTED"}
    )
    private ReportStatus status;

    @Size(max = 1000, message = "processingMemo는 1000자 이하여야 합니다")
    @Schema(description = "처리 메모", example = "동일 대상 반복 신고 이력을 검토 중입니다.")
    private String processingMemo;

    @Size(max = 100, message = "finalAction은 100자 이하여야 합니다")
    @Schema(description = "최종 조치", example = "CONTENT_HIDDEN")
    private String finalAction;
}
