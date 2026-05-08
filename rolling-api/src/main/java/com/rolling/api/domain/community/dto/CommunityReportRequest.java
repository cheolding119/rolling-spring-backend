package com.rolling.api.domain.community.dto;

import com.rolling.api.domain.report.entity.ReportReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "커뮤니티 신고 요청")
public class CommunityReportRequest {

    @NotNull(message = "reason은 필수입니다")
    @Schema(description = "신고 사유", example = "SPAM")
    private ReportReason reason;

    @Size(max = 500, message = "customReason은 500자 이하여야 합니다")
    @Schema(description = "기타 사유", nullable = true)
    private String customReason;
}
