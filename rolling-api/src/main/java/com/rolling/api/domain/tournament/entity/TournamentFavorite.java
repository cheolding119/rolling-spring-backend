package com.rolling.api.domain.tournament.entity;

import com.rolling.api.domain.user.entity.User;
import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(
        name = "tournament_favorites",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tournament_favorites_user_tournament", columnNames = {"user_id", "tournament_id"})
        },
        indexes = {
                @Index(name = "idx_tournament_favorites_user_created_at", columnList = "user_id,created_at"),
                @Index(name = "idx_tournament_favorites_notification_schedule", columnList = "notification_enabled,scheduled_at,sent_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TournamentFavorite extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "notification_enabled", nullable = false)
    private Boolean notificationEnabled;

    @Column(name = "remind_date")
    private LocalDate remindDate;

    @Column(name = "remind_time")
    private LocalTime remindTime;

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Builder
    public TournamentFavorite(User user, Tournament tournament) {
        this.user = user;
        this.tournament = tournament;
        this.notificationEnabled = false;
    }

    public void enableReminder(LocalDate remindDate, LocalTime remindTime, LocalDateTime scheduledAt) {
        this.notificationEnabled = true;
        this.remindDate = remindDate;
        this.remindTime = remindTime;
        this.scheduledAt = scheduledAt;
        this.sentAt = null;
    }

    public void disableReminder() {
        this.notificationEnabled = false;
        this.remindDate = null;
        this.remindTime = null;
        this.scheduledAt = null;
        this.sentAt = null;
    }

    public void markAsSent(LocalDateTime sentAt) {
        if (this.sentAt == null) {
            this.sentAt = sentAt;
        }
    }
}
