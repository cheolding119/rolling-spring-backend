package com.rolling.api.domain.notice.entity;

import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "notices",
        indexes = {
                @Index(name = "idx_notices_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Builder
    public Notice(String title, String content, String authorName, String createdBy) {
        this.title = title;
        this.content = content;
        this.authorName = authorName;
        this.createdBy = createdBy;
    }

    public void update(String title, String content, String authorName, String createdBy) {
        if (title != null) {
            this.title = title;
        }
        if (content != null) {
            this.content = content;
        }
        if (authorName != null) {
            this.authorName = authorName;
        }
        if (createdBy != null) {
            this.createdBy = createdBy;
        }
    }
}
