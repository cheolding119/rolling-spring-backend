package com.rolling.api.domain.community.entity;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_comments")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private Long reportCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityCommentStatus status = CommunityCommentStatus.ACTIVE;

    private LocalDateTime deletedAt;

    @Builder
    public CommunityComment(CommunityPost post,
                            User author,
                            String content,
                            Long reportCount,
                            CommunityCommentStatus status,
                            LocalDateTime deletedAt) {
        this.post = post;
        this.author = author;
        this.content = content;
        if (reportCount != null) {
            this.reportCount = reportCount;
        }
        if (status != null) {
            this.status = status;
        }
        this.deletedAt = deletedAt;
    }

    public void updateContent(String content) {
        if (content != null) {
            this.content = content;
        }
    }

    public void incrementReportCount() {
        this.reportCount = this.reportCount == null ? 1L : this.reportCount + 1L;
    }

    public void delete(LocalDateTime deletedAt) {
        this.status = CommunityCommentStatus.DELETED;
        this.deletedAt = deletedAt;
    }

    public void hide() {
        if (this.status != CommunityCommentStatus.DELETED) {
            this.status = CommunityCommentStatus.HIDDEN;
        }
    }

    public void unhide() {
        if (this.status != CommunityCommentStatus.DELETED) {
            this.status = CommunityCommentStatus.ACTIVE;
        }
    }
}
