package com.rolling.api.domain.traininglog.entity;

import com.rolling.api.domain.user.entity.BeltColor;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "training_log_entries",
        indexes = {
                @Index(name = "idx_training_log_entries_user_date", columnList = "user_id,training_date"),
                @Index(name = "idx_training_log_entries_user_created_at", columnList = "user_id,created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrainingLogEntry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "training_date", nullable = false)
    private LocalDate trainingDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrainingLogCategory category;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(name = "checklist_json", columnDefinition = "TEXT")
    private String checklistJson;

    @Column(name = "hashtags_json", columnDefinition = "TEXT")
    private String hashtagsJson;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "image_urls_json", columnDefinition = "TEXT")
    private String imageUrlsJson;

    @Column(name = "external_links_json", columnDefinition = "TEXT")
    private String externalLinksJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "color")
    private TrainingLogColor color;

    @Column(name = "training_intensity")
    private Integer trainingIntensity;

    @Column(name = "gym_attendance")
    private Boolean gymAttendance;

    @Column(name = "condition")
    private Integer condition;

    @Column(name = "training_minutes")
    private Integer trainingMinutes;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private TrainingLogVisibility visibility = TrainingLogVisibility.PRIVATE;

    @Enumerated(EnumType.STRING)
    @Column(name = "belt_color")
    private BeltColor beltColor;

    @Column(name = "stripe_count")
    private Integer stripeCount;

    @Builder
    public TrainingLogEntry(
            User user,
            LocalDate trainingDate,
            TrainingLogCategory category,
            String title,
            String content,
            String checklistJson,
            String hashtagsJson,
            String imageUrl,
            String imageUrlsJson,
            String externalLinksJson,
            TrainingLogColor color,
            Integer trainingIntensity,
            Boolean gymAttendance,
            Integer condition,
            Integer trainingMinutes,
            TrainingLogVisibility visibility,
            BeltColor beltColor,
            Integer stripeCount
    ) {
        this.user = user;
        this.trainingDate = trainingDate;
        this.category = category;
        this.title = title;
        this.content = content;
        this.checklistJson = checklistJson;
        this.hashtagsJson = hashtagsJson;
        this.imageUrl = imageUrl;
        this.imageUrlsJson = imageUrlsJson;
        this.externalLinksJson = externalLinksJson;
        this.color = color;
        this.trainingIntensity = trainingIntensity;
        this.gymAttendance = gymAttendance;
        this.condition = condition;
        this.trainingMinutes = trainingMinutes;
        this.visibility = visibility == null ? TrainingLogVisibility.PRIVATE : visibility;
        this.beltColor = beltColor;
        this.stripeCount = stripeCount;
    }

    public void update(
            TrainingLogCategory category,
            String title,
            String content,
            String checklistJson,
            String hashtagsJson,
            Integer trainingMinutes,
            Integer trainingIntensity,
            Boolean gymAttendance,
            Integer condition,
            String imageUrl,
            String imageUrlsJson,
            String externalLinksJson,
            TrainingLogColor color,
            TrainingLogVisibility visibility,
            BeltColor beltColor,
            Integer stripeCount
    ) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.checklistJson = checklistJson;
        this.hashtagsJson = hashtagsJson;
        this.trainingMinutes = trainingMinutes;
        this.trainingIntensity = trainingIntensity;
        this.gymAttendance = gymAttendance;
        this.condition = condition;
        this.imageUrl = imageUrl;
        this.imageUrlsJson = imageUrlsJson;
        this.externalLinksJson = externalLinksJson;
        this.color = color;
        this.visibility = visibility == null ? TrainingLogVisibility.PRIVATE : visibility;
        this.beltColor = beltColor;
        this.stripeCount = stripeCount;
    }

    public void update(
            TrainingLogCategory category,
            String title,
            String content,
            String checklistJson,
            String hashtagsJson,
            Integer trainingMinutes,
            Integer trainingIntensity,
            Boolean gymAttendance,
            Integer condition,
            String imageUrl,
            String imageUrlsJson,
            String externalLinksJson,
            TrainingLogColor color,
            BeltColor beltColor,
            Integer stripeCount
    ) {
        update(
                category,
                title,
                content,
                checklistJson,
                hashtagsJson,
                trainingMinutes,
                trainingIntensity,
                gymAttendance,
                condition,
                imageUrl,
                imageUrlsJson,
                externalLinksJson,
                color,
                this.visibility,
                beltColor,
                stripeCount
        );
    }

    public void update(
            TrainingLogCategory category,
            String title,
            String content,
            String checklistJson,
            String hashtagsJson,
            Integer trainingMinutes,
            Integer trainingIntensity,
            String imageUrl,
            String imageUrlsJson,
            String externalLinksJson,
            TrainingLogColor color,
            TrainingLogVisibility visibility,
            BeltColor beltColor,
            Integer stripeCount
    ) {
        update(
                category,
                title,
                content,
                checklistJson,
                hashtagsJson,
                trainingMinutes,
                trainingIntensity,
                null,
                null,
                imageUrl,
                imageUrlsJson,
                externalLinksJson,
                color,
                visibility,
                beltColor,
                stripeCount
        );
    }

    public void update(
            TrainingLogCategory category,
            String title,
            String content,
            String checklistJson,
            String hashtagsJson,
            Integer trainingMinutes,
            Integer trainingIntensity,
            String imageUrl,
            String imageUrlsJson,
            String externalLinksJson,
            TrainingLogColor color,
            BeltColor beltColor,
            Integer stripeCount
    ) {
        update(
                category,
                title,
                content,
                checklistJson,
                hashtagsJson,
                trainingMinutes,
                trainingIntensity,
                imageUrl,
                imageUrlsJson,
                externalLinksJson,
                color,
                this.visibility,
                beltColor,
                stripeCount
        );
    }

    public void update(
            TrainingLogCategory category,
            String title,
            String content,
            String checklistJson,
            String hashtagsJson,
            String imageUrl,
            String imageUrlsJson,
            String externalLinksJson,
            TrainingLogColor color,
            TrainingLogVisibility visibility,
            BeltColor beltColor,
            Integer stripeCount
    ) {
        update(
                category,
                title,
                content,
                checklistJson,
                hashtagsJson,
                null,
                null,
                null,
                null,
                imageUrl,
                imageUrlsJson,
                externalLinksJson,
                color,
                visibility,
                beltColor,
                stripeCount
        );
    }

    public void update(
            TrainingLogCategory category,
            String title,
            String content,
            String checklistJson,
            String hashtagsJson,
            String imageUrl,
            String imageUrlsJson,
            String externalLinksJson,
            TrainingLogColor color,
            BeltColor beltColor,
            Integer stripeCount
    ) {
        update(
                category,
                title,
                content,
                checklistJson,
                hashtagsJson,
                imageUrl,
                imageUrlsJson,
                externalLinksJson,
                color,
                this.visibility,
                beltColor,
                stripeCount
        );
    }

    public void update(
            TrainingLogCategory category,
            String title,
            String content,
            String checklistJson,
            String hashtagsJson,
            String imageUrl,
            String externalLinksJson,
            TrainingLogColor color,
            TrainingLogVisibility visibility,
            BeltColor beltColor,
            Integer stripeCount
    ) {
        update(
                category,
                title,
                content,
                checklistJson,
                hashtagsJson,
                null,
                null,
                null,
                null,
                imageUrl,
                null,
                externalLinksJson,
                color,
                visibility,
                beltColor,
                stripeCount
        );
    }

    public void update(
            TrainingLogCategory category,
            String title,
            String content,
            String checklistJson,
            String hashtagsJson,
            String imageUrl,
            String externalLinksJson,
            TrainingLogColor color,
            BeltColor beltColor,
            Integer stripeCount
    ) {
        update(
                category,
                title,
                content,
                checklistJson,
                hashtagsJson,
                imageUrl,
                externalLinksJson,
                color,
                this.visibility,
                beltColor,
                stripeCount
        );
    }
}
