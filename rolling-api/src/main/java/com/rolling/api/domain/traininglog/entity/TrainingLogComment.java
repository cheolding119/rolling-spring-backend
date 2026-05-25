package com.rolling.api.domain.traininglog.entity;

import com.rolling.api.domain.user.entity.User;
import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "training_log_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingLogComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "entry_id", nullable = false)
    private TrainingLogEntry entry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    private TrainingLogComment parentComment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_user_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private Long reportCount = 0L;

    @Builder
    public TrainingLogComment(
            TrainingLogEntry entry,
            TrainingLogComment parentComment,
            User author,
            String content,
            boolean deleted,
            LocalDateTime deletedAt,
            Long reportCount
    ) {
        this.entry = entry;
        this.parentComment = parentComment;
        this.author = author;
        this.content = content;
        this.deleted = deleted;
        this.deletedAt = deletedAt;
        if (reportCount != null) {
            this.reportCount = reportCount;
        }
    }

    public boolean isReply() {
        return parentComment != null;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void incrementReportCount() {
        this.reportCount = this.reportCount == null ? 1L : this.reportCount + 1L;
    }

    public void softDelete(LocalDateTime deletedAt) {
        this.deleted = true;
        this.deletedAt = deletedAt;
        this.content = null;
    }
}
