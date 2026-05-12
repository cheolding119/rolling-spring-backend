package com.rolling.api.domain.seminar.entity;

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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "seminar_applications",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_seminar_applications_seminar_user", columnNames = {"seminar_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_seminar_applications_user_status", columnList = "user_id,status"),
                @Index(name = "idx_seminar_applications_seminar_status", columnList = "seminar_id,status")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeminarApplication extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seminar_id", nullable = false)
    private Seminar seminar;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeminarApplicationStatus status = SeminarApplicationStatus.APPLIED;

    @Column(length = 500)
    private String cancelReason;

    @Column(nullable = false)
    private LocalDateTime appliedAt;

    private LocalDateTime canceledAt;

    @Builder
    public SeminarApplication(Seminar seminar,
                              User user,
                              SeminarApplicationStatus status,
                              String cancelReason,
                              LocalDateTime appliedAt,
                              LocalDateTime canceledAt) {
        this.seminar = seminar;
        this.user = user;
        if (status != null) {
            this.status = status;
        }
        this.cancelReason = cancelReason;
        this.appliedAt = appliedAt;
        this.canceledAt = canceledAt;
    }

    public boolean isApplied() {
        return status == SeminarApplicationStatus.APPLIED;
    }

    public void reactivate(LocalDateTime appliedAt) {
        this.status = SeminarApplicationStatus.APPLIED;
        this.appliedAt = appliedAt;
        this.cancelReason = null;
        this.canceledAt = null;
    }

    public void cancel(String cancelReason, LocalDateTime canceledAt) {
        this.status = SeminarApplicationStatus.CANCELED;
        this.cancelReason = normalize(cancelReason);
        this.canceledAt = canceledAt;
    }

    public void cancelBySeminar(LocalDateTime canceledAt) {
        this.status = SeminarApplicationStatus.SEMINAR_CANCELED;
        this.cancelReason = null;
        this.canceledAt = canceledAt;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
