package com.rolling.api.domain.seminar.entity;

import com.rolling.api.domain.openmat.entity.Region;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "seminars",
        indexes = {
                @Index(name = "idx_seminars_status_start", columnList = "status,start_date_time"),
                @Index(name = "idx_seminars_region_start", columnList = "region,start_date_time"),
                @Index(name = "idx_seminars_host", columnList = "host_id")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Seminar extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(length = 1000)
    private String mainImageUrl;

    @Column(nullable = false)
    private String instructorName;

    @Column(columnDefinition = "TEXT")
    private String instructorBio;

    @Column(columnDefinition = "TEXT")
    private String curriculum;

    @Column(columnDefinition = "TEXT")
    private String targetAudience;

    @Column(columnDefinition = "TEXT")
    private String preparation;

    private String contactInfo;

    private String hostInstagramId;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    private LocalDateTime applicationStartDateTime;

    private LocalDateTime applicationEndDateTime;

    @Column(nullable = false)
    private String locationName;

    @Column(nullable = false)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;

    @Column(nullable = false)
    private Integer maxCapacity = -1;

    @Column(nullable = false)
    private Integer price = 0;

    @Column(columnDefinition = "TEXT")
    private String paymentGuide;

    @Column(columnDefinition = "TEXT")
    private String refundPolicy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeminarStatus status = SeminarStatus.RECRUITING;

    @Column(nullable = false)
    private Integer reportCount = 0;

    @Column(nullable = false)
    private Boolean isHidden = false;

    private LocalDateTime deletedAt;

    @Builder
    public Seminar(User host,
                   String title,
                   String description,
                   String mainImageUrl,
                   String instructorName,
                   String instructorBio,
                   String curriculum,
                   String targetAudience,
                   String preparation,
                   String contactInfo,
                   String hostInstagramId,
                   LocalDateTime startDateTime,
                   LocalDateTime endDateTime,
                   LocalDateTime applicationStartDateTime,
                   LocalDateTime applicationEndDateTime,
                   String locationName,
                   String address,
                   BigDecimal latitude,
                   BigDecimal longitude,
                   Region region,
                   Integer maxCapacity,
                   Integer price,
                   String paymentGuide,
                   String refundPolicy,
                   SeminarStatus status) {
        this.host = host;
        this.title = title;
        this.description = description;
        this.mainImageUrl = mainImageUrl;
        this.instructorName = instructorName;
        this.instructorBio = instructorBio;
        this.curriculum = curriculum;
        this.targetAudience = targetAudience;
        this.preparation = preparation;
        this.contactInfo = contactInfo;
        this.hostInstagramId = hostInstagramId;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.applicationStartDateTime = applicationStartDateTime;
        this.applicationEndDateTime = applicationEndDateTime;
        this.locationName = locationName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
        this.region = region;
        if (maxCapacity != null) {
            this.maxCapacity = maxCapacity;
        }
        if (price != null) {
            this.price = price;
        }
        this.paymentGuide = paymentGuide;
        this.refundPolicy = refundPolicy;
        if (status != null) {
            this.status = status;
        }
    }

    public void update(String title,
                       String description,
                       String mainImageUrl,
                       String instructorName,
                       String instructorBio,
                       String curriculum,
                       String targetAudience,
                       String preparation,
                       String contactInfo,
                       String hostInstagramId,
                       LocalDateTime startDateTime,
                       LocalDateTime endDateTime,
                       LocalDateTime applicationStartDateTime,
                       LocalDateTime applicationEndDateTime,
                       String locationName,
                       String address,
                       Region region,
                       Integer maxCapacity,
                       Integer price,
                       String paymentGuide,
                       String refundPolicy) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (mainImageUrl != null) this.mainImageUrl = mainImageUrl;
        if (instructorName != null) this.instructorName = instructorName;
        if (instructorBio != null) this.instructorBio = instructorBio;
        if (curriculum != null) this.curriculum = curriculum;
        if (targetAudience != null) this.targetAudience = targetAudience;
        if (preparation != null) this.preparation = preparation;
        if (contactInfo != null) this.contactInfo = contactInfo;
        if (hostInstagramId != null) this.hostInstagramId = hostInstagramId;
        if (startDateTime != null) this.startDateTime = startDateTime;
        if (endDateTime != null) this.endDateTime = endDateTime;
        if (applicationStartDateTime != null) this.applicationStartDateTime = applicationStartDateTime;
        if (applicationEndDateTime != null) this.applicationEndDateTime = applicationEndDateTime;
        if (locationName != null) this.locationName = locationName;
        if (address != null) this.address = address;
        if (region != null) this.region = region;
        if (maxCapacity != null) this.maxCapacity = maxCapacity;
        if (price != null) this.price = price;
        if (paymentGuide != null) this.paymentGuide = paymentGuide;
        if (refundPolicy != null) this.refundPolicy = refundPolicy;
    }

    public void updateCoordinates(BigDecimal latitude, BigDecimal longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void hide(LocalDateTime deletedAt) {
        this.isHidden = true;
        this.deletedAt = deletedAt;
        this.status = SeminarStatus.DELETED;
    }

    public void synchronizeStatus(LocalDateTime now, int appliedCount) {
        if (now == null || Boolean.TRUE.equals(isHidden) || status == SeminarStatus.CANCELED) {
            return;
        }
        if (!endDateTime.isAfter(now)) {
            this.status = SeminarStatus.FINISHED;
            return;
        }
        if (applicationEndDateTime != null && !applicationEndDateTime.isAfter(now)) {
            this.status = SeminarStatus.CLOSED;
            return;
        }
        if (isCapacityFull(appliedCount)) {
            this.status = SeminarStatus.CLOSED;
            return;
        }
        this.status = SeminarStatus.RECRUITING;
    }

    public boolean isCapacityFull(int appliedCount) {
        return maxCapacity != null && maxCapacity != -1 && appliedCount >= maxCapacity;
    }

    public boolean isApplicationNotOpen(LocalDateTime now) {
        return applicationStartDateTime != null && now.isBefore(applicationStartDateTime);
    }

    public boolean isApplicationClosed(LocalDateTime now) {
        if (!startDateTime.isAfter(now)) {
            return true;
        }
        return applicationEndDateTime != null && !applicationEndDateTime.isAfter(now);
    }

    public int remainingCapacity(int appliedCount) {
        if (maxCapacity == null || maxCapacity == -1) {
            return -1;
        }
        return Math.max(maxCapacity - appliedCount, 0);
    }
}
