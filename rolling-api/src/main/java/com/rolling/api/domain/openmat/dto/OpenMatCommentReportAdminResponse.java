package com.rolling.api.domain.openmat.dto;

import com.rolling.api.domain.openmat.entity.OpenMatCommentReport;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "오픈매트 댓글 신고 관리자 응답")
public class OpenMatCommentReportAdminResponse {

    @Schema(description = "신고 ID")
    private Long id;

    @Schema(description = "댓글 ID")
    private Long commentId;

    @Schema(description = "오픈매트 ID")
    private Long openMatId;

    @Schema(description = "오픈매트 제목")
    private String openMatTitle;

    @Schema(description = "상위 댓글 ID")
    private Long parentCommentId;

    @Schema(description = "댓글 본문")
    private String commentContent;

    @Schema(description = "댓글 삭제 여부")
    private boolean commentDeleted;

    @Schema(description = "댓글 작성자 ID")
    private Long commentAuthorUserId;

    @Schema(description = "댓글 작성자 닉네임")
    private String commentAuthorNickname;

    @Schema(description = "신고자 ID")
    private Long reporterUserId;

    @Schema(description = "신고자 닉네임")
    private String reporterNickname;

    @Schema(description = "신고 사유")
    private ReportReason reason;

    @Schema(description = "기타 사유")
    private String customReason;

    @Schema(description = "처리 상태")
    private ReportStatus status;

    @Schema(description = "처리한 관리자 ID")
    private Long processedByUserId;

    @Schema(description = "처리 시각")
    private LocalDateTime processedAt;

    @Schema(description = "처리 메모")
    private String processingMemo;

    @Schema(description = "최종 조치")
    private String finalAction;

    @Schema(description = "생성 시각")
    private LocalDateTime createdAt;

    @Schema(description = "수정 시각")
    private LocalDateTime updatedAt;

    public static OpenMatCommentReportAdminResponse from(OpenMatCommentReport report) {
        return OpenMatCommentReportAdminResponse.builder()
                .id(report.getId())
                .commentId(report.getComment().getId())
                .openMatId(report.getComment().getOpenMat().getId())
                .openMatTitle(report.getComment().getOpenMat().getTitle())
                .parentCommentId(report.getComment().getParentComment() != null ? report.getComment().getParentComment().getId() : null)
                .commentContent(report.getComment().getContent())
                .commentDeleted(report.getComment().isDeleted())
                .commentAuthorUserId(report.getComment().getAuthor().getId())
                .commentAuthorNickname(report.getComment().getAuthor().getNickname())
                .reporterUserId(report.getReporter().getId())
                .reporterNickname(report.getReporter().getNickname())
                .reason(report.getReason())
                .customReason(report.getCustomReason())
                .status(report.getStatus())
                .processedByUserId(report.getProcessedByUserId())
                .processedAt(report.getProcessedAt())
                .processingMemo(report.getProcessingMemo())
                .finalAction(report.getFinalAction())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .build();
    }
}
