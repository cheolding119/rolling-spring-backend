package com.rolling.api.domain.traininglog.entity;

import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "training_cards",
        indexes = {
                @Index(name = "idx_training_cards_active_display_order", columnList = "active,display_order"),
                @Index(name = "idx_training_cards_level_position", columnList = "level,position")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingCard extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, length = 500)
    private String summary;

    @Column(name = "topic", nullable = false, length = 100)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrainingCardLevel level;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TrainingCardPosition position;

    @Column(name = "situation_summary", nullable = false, length = 255)
    private String situationSummary;

    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "situation_description", nullable = false, columnDefinition = "TEXT")
    private String situationDescription;

    @Column(name = "starting_position_description", nullable = false, columnDefinition = "TEXT")
    private String startingPositionDescription;

    @Column(name = "flow_description", nullable = false, columnDefinition = "TEXT")
    private String flowDescription;

    @Column(name = "key_points", nullable = false, columnDefinition = "TEXT")
    private String keyPoints;

    @Column(name = "common_mistakes", nullable = false, columnDefinition = "TEXT")
    private String commonMistakes;

    @Column(name = "cautions", nullable = false, columnDefinition = "TEXT")
    private String cautions;

    @Column(name = "youtube_url", length = 1000)
    private String youtubeUrl;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Builder
    public TrainingCard(
            String title,
            String summary,
            String topic,
            TrainingCardLevel level,
            TrainingCardPosition position,
            String situationSummary,
            String description,
            String situationDescription,
            String startingPositionDescription,
            String flowDescription,
            String keyPoints,
            String commonMistakes,
            String cautions,
            String youtubeUrl,
            boolean active,
            int displayOrder
    ) {
        this.title = title;
        this.summary = summary;
        this.topic = topic;
        this.level = level;
        this.position = position;
        this.situationSummary = situationSummary;
        this.description = description;
        this.situationDescription = situationDescription;
        this.startingPositionDescription = startingPositionDescription;
        this.flowDescription = flowDescription;
        this.keyPoints = keyPoints;
        this.commonMistakes = commonMistakes;
        this.cautions = cautions;
        this.youtubeUrl = youtubeUrl;
        this.active = active;
        this.displayOrder = displayOrder;
    }
}
