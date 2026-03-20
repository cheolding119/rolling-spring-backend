package com.rolling.api.domain.report.entity;

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
        name = "reports",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"reporter_id", "target_type", "target_id"})
        },
        indexes = {
                @Index(name = "idx_reports_target", columnList = "target_type,target_id"),
                @Index(name = "idx_reports_status_created_at", columnList = "status,created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Report extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false)
    private ReportTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportReason reason;

    @Column(length = 500)
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
    public Report(User reporter,
                  ReportTargetType targetType,
                  Long targetId,
                  ReportReason reason,
                  String customReason,
                  ReportStatus status,
                  Long processedByUserId,
                  LocalDateTime processedAt,
                  String processingMemo,
                  String finalAction) {
        this.reporter = reporter;
        this.targetType = targetType;
        this.targetId = targetId;
        this.reason = reason;
        this.customReason = customReason;
        this.status = status;
        this.processedByUserId = processedByUserId;
        this.processedAt = processedAt;
        this.processingMemo = processingMemo;
        this.finalAction = finalAction;
    }

    public void updateStatus(ReportStatus status,
                             Long processedByUserId,
                             LocalDateTime processedAt,
                             String processingMemo,
                             String finalAction) {
        this.status = status;
        this.processedByUserId = processedByUserId;
        this.processedAt = processedAt;
        this.processingMemo = processingMemo;
        this.finalAction = finalAction;
    }
}
