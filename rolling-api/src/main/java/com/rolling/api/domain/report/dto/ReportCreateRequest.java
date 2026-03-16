package com.rolling.api.domain.report.dto;

import com.rolling.api.domain.report.entity.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "신고 요청")
public class ReportCreateRequest {

    @NotNull(message = "reason은 필수입니다")
    @Schema(
            description = "신고 사유",
            example = "SPAM",
            allowableValues = {"FALSE_INFO", "INAPPROPRIATE", "SPAM", "OTHER"}
    )
    private ReportReason reason;

    @Size(max = 500, message = "customReason은 500자 이하여야 합니다")
    @Schema(description = "기타 신고 사유", example = "광고성 게시물입니다")
    private String customReason;
}
