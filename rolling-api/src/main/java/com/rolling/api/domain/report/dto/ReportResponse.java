package com.rolling.api.domain.report.dto;

import com.rolling.api.domain.report.entity.Report;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.report.entity.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "신고 응답")
public class ReportResponse {

    @Schema(description = "신고 ID", example = "101")
    private Long id;

    @Schema(description = "신고자 사용자 ID", example = "22")
    private Long reporterUserId;

    @Schema(description = "신고자 닉네임", example = "rolling_user")
    private String reporterNickname;

    @Schema(description = "신고 대상 타입", example = "OPEN_MAT")
    private ReportTargetType targetType;

    @Schema(description = "신고 대상 ID", example = "3001")
    private Long targetId;

    @Schema(description = "신고 사유", example = "SPAM")
    private ReportReason reason;

    @Schema(description = "기타 신고 사유")
    private String customReason;

    @Schema(description = "신고 처리 상태", example = "RECEIVED")
    private ReportStatus status;

    @Schema(description = "처리한 관리자 사용자 ID", example = "1")
    private Long processedByUserId;

    @Schema(description = "처리 시각")
    private LocalDateTime processedAt;

    @Schema(description = "처리 메모")
    private String processingMemo;

    @Schema(description = "최종 조치")
    private String finalAction;

    @Schema(description = "동일 대상 누적 현황")
    private ReportTargetSummary targetSummary;

    @Schema(description = "신고 생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "신고 수정 시각")
    private LocalDateTime updatedAt;

    public static ReportResponse from(Report report, ReportTargetSummary targetSummary) {
        return ReportResponse.builder()
                .id(report.getId())
                .reporterUserId(report.getReporter().getId())
                .reporterNickname(report.getReporter().getNickname())
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .reason(report.getReason())
                .customReason(report.getCustomReason())
                .status(report.getStatus())
                .processedByUserId(report.getProcessedByUserId())
                .processedAt(report.getProcessedAt())
                .processingMemo(report.getProcessingMemo())
                .finalAction(report.getFinalAction())
                .targetSummary(targetSummary)
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
