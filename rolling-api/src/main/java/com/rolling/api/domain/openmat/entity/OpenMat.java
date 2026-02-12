package com.rolling.api.domain.openmat.entity;

import com.rolling.api.domain.user.entity.User;
import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private LocalDateTime startDateTime;

    @Column(nullable = false)
    private LocalDateTime endDateTime;

    private String locationName;

    private String address;

    @ElementCollection
    @CollectionTable(name = "open_mat_participants", joinColumns = @JoinColumn(name = "open_mat_id"))
    @Column(name = "user_id")
    private List<Long> participantUids = new ArrayList<>();

    @Column(nullable = false)
    private Integer maxCapacity = -1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OpenMatStatus status = OpenMatStatus.RECRUITING;

    @Column(nullable = false)
    private Integer reportCount = 0;

    @Column(nullable = false)
    private Boolean isHidden = false;

    private String hostInstagramId;

    @Builder
    public OpenMat(User host,
                   String title,
                   String description,
                   LocalDateTime startDateTime,
                   LocalDateTime endDateTime,
                   String locationName,
                   String address,
                   Integer maxCapacity,
                   String hostInstagramId,
                   OpenMatStatus status) {
        this.host = host;
        this.title = title;
        this.description = description;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.locationName = locationName;
        this.address = address;
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

    public void report() {
        this.reportCount++;
        if (this.reportCount >= 2) {
            this.isHidden = true;
        }
    }
}
