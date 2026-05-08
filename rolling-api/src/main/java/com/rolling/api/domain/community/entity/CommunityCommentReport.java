package com.rolling.api.domain.community.entity;

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

@Entity
@Table(
        name = "community_comment_reports",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"comment_id", "reporter_id"})
        },
        indexes = {
                @Index(name = "idx_community_comment_reports_comment", columnList = "comment_id"),
                @Index(name = "idx_community_comment_reports_reporter", columnList = "reporter_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityCommentReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private CommunityComment comment;

    @ManyToOne(fetch = FetchType.LAZY)
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

    @Builder
    public CommunityCommentReport(CommunityComment comment,
                                  User reporter,
                                  ReportReason reason,
                                  String customReason,
                                  ReportStatus status) {
        this.comment = comment;
        this.reporter = reporter;
        this.reason = reason;
        this.customReason = customReason;
        this.status = status;
    }
}
