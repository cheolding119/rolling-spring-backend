package com.rolling.api.domain.openmat.entity;

import com.rolling.api.domain.user.entity.User;
import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "open_mats")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OpenMat extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id", nullable = false)
    private User host;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String region;

    private String locationName;

    private String address;

    @Column(nullable = false)
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    @Column(nullable = false)
    private Integer currentParticipants = 0;

    @Column(nullable = false)
    private Integer maxCapacity = -1;

    private String hostInstagramId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpenMatStatus status = OpenMatStatus.RECRUITING;

    @Builder
    public OpenMat(User host,
                   String title,
                   String description,
                   String region,
                   String locationName,
                   String address,
                   LocalDateTime startDateTime,
                   LocalDateTime endDateTime,
                   Integer currentParticipants,
                   Integer maxCapacity,
                   String hostInstagramId,
                   OpenMatStatus status) {
        this.host = host;
        this.title = title;
        this.description = description;
        this.region = region;
        this.locationName = locationName;
        this.address = address;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        if (currentParticipants != null) {
            this.currentParticipants = currentParticipants;
        }
        if (maxCapacity != null) {
            this.maxCapacity = maxCapacity;
        }
        if (hostInstagramId != null && !hostInstagramId.isBlank()) {
            this.hostInstagramId = hostInstagramId;
        }
        if (status != null) {
            this.status = status;
        }
    }
}
