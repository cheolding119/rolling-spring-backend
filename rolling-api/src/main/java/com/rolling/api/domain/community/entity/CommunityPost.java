package com.rolling.api.domain.community.entity;

import com.rolling.api.domain.user.entity.User;
import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDateTime;

@Entity
@Table(name = "community_posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private CommunityPostCategory category;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false)
    private Long viewCount = 0L;

    @Column(nullable = false)
    private Long likeCount = 0L;

    @Column(nullable = false)
    private Long commentCount = 0L;

    @Column(nullable = false)
    private Long reportCount = 0L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CommunityPostStatus status = CommunityPostStatus.ACTIVE;

    private LocalDateTime deletedAt;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommunityPostImage> images = new ArrayList<>();

    @Builder
    public CommunityPost(User author,
                         CommunityPostCategory category,
                         String title,
                         String content,
                         Long viewCount,
                         Long likeCount,
                         Long commentCount,
                         Long reportCount,
                         CommunityPostStatus status,
                         LocalDateTime deletedAt) {
        this.author = author;
        this.category = category;
        this.title = title;
        this.content = content;
        if (viewCount != null) {
            this.viewCount = viewCount;
        }
        if (likeCount != null) {
            this.likeCount = likeCount;
        }
        if (commentCount != null) {
            this.commentCount = commentCount;
        }
        if (reportCount != null) {
            this.reportCount = reportCount;
        }
        if (status != null) {
            this.status = status;
        }
        this.deletedAt = deletedAt;
    }

    public void update(CommunityPostCategory category, String title, String content) {
        if (category != null) {
            this.category = category;
        }
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
    }

    public void incrementViewCount() {
        this.viewCount = this.viewCount == null ? 1L : this.viewCount + 1L;
    }

    public void incrementCommentCount() {
        this.commentCount = this.commentCount == null ? 1L : this.commentCount + 1L;
    }

    public void decrementCommentCount() {
        if (this.commentCount == null || this.commentCount <= 0L) {
            this.commentCount = 0L;
            return;
        }
        this.commentCount = this.commentCount - 1L;
    }

    public void incrementLikeCount() {
        this.likeCount = this.likeCount == null ? 1L : this.likeCount + 1L;
    }

    public void decrementLikeCount() {
        if (this.likeCount == null || this.likeCount <= 0L) {
            this.likeCount = 0L;
            return;
        }
        this.likeCount = this.likeCount - 1L;
    }

    public void incrementReportCount() {
        this.reportCount = this.reportCount == null ? 1L : this.reportCount + 1L;
    }

    public void replaceImages(List<CommunityPostImage> newImages) {
        this.images.clear();
        if (newImages == null || newImages.isEmpty()) {
            return;
        }
        for (CommunityPostImage image : newImages) {
            image.assignPost(this);
            this.images.add(image);
        }
    }

    public void delete(LocalDateTime deletedAt) {
        this.status = CommunityPostStatus.DELETED;
        this.deletedAt = deletedAt;
    }

    public void hide() {
        if (this.status != CommunityPostStatus.DELETED) {
            this.status = CommunityPostStatus.HIDDEN;
        }
    }

    public void unhide() {
        if (this.status != CommunityPostStatus.DELETED) {
            this.status = CommunityPostStatus.ACTIVE;
        }
    }
}
