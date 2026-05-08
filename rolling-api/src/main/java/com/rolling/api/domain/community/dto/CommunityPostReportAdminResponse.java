package com.rolling.api.domain.community.dto;

import com.rolling.api.domain.community.entity.CommunityPostReport;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "커뮤니티 게시글 신고 관리자 응답")
public class CommunityPostReportAdminResponse {

    @Schema(description = "신고 ID")
    private Long id;

    @Schema(description = "게시글 ID")
    private Long postId;

    @Schema(description = "게시글 제목")
    private String postTitle;

    @Schema(description = "게시글 작성자 닉네임")
    private String postAuthorNickname;

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

    public static CommunityPostReportAdminResponse from(CommunityPostReport report) {
        return CommunityPostReportAdminResponse.builder()
                .id(report.getId())
                .postId(report.getPost().getId())
                .postTitle(report.getPost().getTitle())
                .postAuthorNickname(report.getPost().getAuthor().getCommunityNickname())
                .reporterUserId(report.getReporter().getId())
                .reporterNickname(report.getReporter().getCommunityNickname())
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
