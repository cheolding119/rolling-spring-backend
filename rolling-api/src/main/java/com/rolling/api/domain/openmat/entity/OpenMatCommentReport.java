package com.rolling.api.domain.openmat.entity;

import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "open_mat_comment_reports",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"comment_id", "reporter_id"})
        },
        indexes = {
                @Index(name = "idx_open_mat_comment_reports_comment", columnList = "comment_id"),
                @Index(name = "idx_open_mat_comment_reports_reporter", columnList = "reporter_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenMatCommentReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private OpenMatComment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReportReason reason;

    @Column(name = "custom_reason", length = 500)
    private String customReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReportStatus status;

    @Column(name = "processed_by_user_id")
    private Long processedByUserId;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "processing_memo", length = 1000)
    private String processingMemo;

    @Column(name = "final_action", length = 100)
    private String finalAction;

    @Builder
    public OpenMatCommentReport(
            OpenMatComment comment,
            User reporter,
            ReportReason reason,
            String customReason,
            ReportStatus status,
            Long processedByUserId,
            LocalDateTime processedAt,
            String processingMemo,
            String finalAction
    ) {
        this.comment = comment;
        this.reporter = reporter;
        this.reason = reason;
        this.customReason = customReason;
        this.status = status;
        this.processedByUserId = processedByUserId;
        this.processedAt = processedAt;
        this.processingMemo = processingMemo;
        this.finalAction = finalAction;
    }

    public void updateStatus(
            ReportStatus status,
            Long processedByUserId,
            LocalDateTime processedAt,
            String processingMemo,
            String finalAction
    ) {
        this.status = status;
        this.processedByUserId = processedByUserId;
        this.processedAt = processedAt;
        this.processingMemo = processingMemo;
        this.finalAction = finalAction;
    }
}
