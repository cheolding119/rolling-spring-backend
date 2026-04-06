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

    private static final int REPORT_BLOCK_THRESHOLD = 3;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Region region;

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

    private LocalDateTime deletedAt;

    @Column(nullable = false)
    private Boolean manualClosed = false;

    private String hostInstagramId;

    @Builder
    public OpenMat(User host,
                   String title,
                   String description,
                   LocalDateTime startDateTime,
                   LocalDateTime endDateTime,
                   String locationName,
                   String address,
                   Region region,
                   Integer maxCapacity,
                   String hostInstagramId,
                   OpenMatStatus status,
                   Boolean manualClosed) {
        this.host = host;
        this.title = title;
        this.description = description;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.locationName = locationName;
        this.address = address;
        this.region = region;
        if (maxCapacity != null) {
            this.maxCapacity = maxCapacity;
        }
        if (hostInstagramId != null && !hostInstagramId.isBlank()) {
            this.hostInstagramId = hostInstagramId;
        }
        if (status != null) {
            this.status = status;
        }
        if (manualClosed != null) {
            this.manualClosed = manualClosed;
        }
    }

    public void update(String title,
                       String description,
                       LocalDateTime startDateTime,
                       LocalDateTime endDateTime,
                       String locationName,
                       String address,
                       Region region,
                       Integer maxCapacity,
                       String hostInstagramId) {
        if (title != null) this.title = title;
        if (description != null) this.description = description;
        if (startDateTime != null) this.startDateTime = startDateTime;
        if (endDateTime != null) this.endDateTime = endDateTime;
        if (locationName != null) this.locationName = locationName;
        if (address != null) this.address = address;
        if (region != null) this.region = region;
        if (maxCapacity != null) this.maxCapacity = maxCapacity;
        if (hostInstagramId != null) this.hostInstagramId = hostInstagramId;
    }

    public boolean hasParticipants() {
        return participantUids != null && !participantUids.isEmpty();
    }

    public boolean isParticipant(Long userId) {
        return participantUids.contains(userId);
    }

    public boolean isCapacityFull() {
        return maxCapacity != -1 && participantUids.size() >= maxCapacity;
    }

    public boolean isCapacityFull(int participantCount) {
        return maxCapacity != -1 && participantCount >= maxCapacity;
    }

    public boolean isReported() {
        return reportCount != null && reportCount >= REPORT_BLOCK_THRESHOLD;
    }

    public void addParticipant(Long userId) {
        participantUids.add(userId);
    }

    public void removeParticipant(Long userId) {
        participantUids.remove(userId);
    }

    public void hide(LocalDateTime deletedAt) {
        this.isHidden = true;
        this.deletedAt = deletedAt;
    }

    public void closeRecruitmentManually() {
        this.manualClosed = true;
        this.status = OpenMatStatus.CLOSED;
    }

    public void reopenRecruitmentManually() {
        this.manualClosed = false;
        this.status = OpenMatStatus.RECRUITING;
    }

    public void synchronizeStatus(LocalDateTime now) {
        synchronizeStatus(now, participantUids.size());
    }

    public void synchronizeStatus(LocalDateTime now, int participantCount) {
        if (now == null) {
            return;
        }

        if (!endDateTime.isAfter(now)) {
            this.status = OpenMatStatus.FINISHED;
            return;
        }

        if (Boolean.TRUE.equals(manualClosed)) {
            this.status = OpenMatStatus.CLOSED;
            return;
        }

        if (isCapacityFull(participantCount)) {
            this.status = OpenMatStatus.CLOSED;
            return;
        }

        this.status = OpenMatStatus.RECRUITING;
    }

    public void report() {
        this.reportCount++;
    }

    public void clearReportBlock() {
        this.reportCount = 0;
    }
}

