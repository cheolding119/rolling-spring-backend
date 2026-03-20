package com.rolling.api.domain.report.dto;

import com.rolling.api.domain.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "동일 신고 대상 누적 현황")
public class ReportTargetSummary {

    @Schema(description = "신고 대상 타입", example = "OPEN_MAT")
    private ReportTargetType targetType;

    @Schema(description = "신고 대상 ID", example = "3001")
    private Long targetId;

    @Schema(description = "전체 신고 수", example = "4")
    private long totalReportCount;

    @Schema(description = "RECEIVED 상태 신고 수", example = "1")
    private long receivedCount;

    @Schema(description = "IN_REVIEW 상태 신고 수", example = "1")
    private long inReviewCount;

    @Schema(description = "RESOLVED 상태 신고 수", example = "2")
    private long resolvedCount;

    @Schema(description = "REJECTED 상태 신고 수", example = "0")
    private long rejectedCount;
}
