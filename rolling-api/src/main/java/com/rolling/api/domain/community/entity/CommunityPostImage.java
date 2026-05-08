package com.rolling.api.domain.community.entity;

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

@Entity
@Table(name = "community_post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private CommunityPost post;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder
    public CommunityPostImage(CommunityPost post, String imageUrl, Integer sortOrder) {
        this.post = post;
        this.imageUrl = imageUrl;
        this.sortOrder = sortOrder;
    }

    void assignPost(CommunityPost post) {
        this.post = post;
    }
}
