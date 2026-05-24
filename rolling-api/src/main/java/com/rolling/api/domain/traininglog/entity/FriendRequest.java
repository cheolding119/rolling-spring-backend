package com.rolling.api.domain.traininglog.entity;

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
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "friend_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FriendRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_user_id", nullable = false)
    private User sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiver;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FriendRequestStatus status;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Builder
    public FriendRequest(User sender, User receiver, FriendRequestStatus status, LocalDateTime respondedAt) {
        this.sender = sender;
        this.receiver = receiver;
        this.status = status == null ? FriendRequestStatus.PENDING : status;
        this.respondedAt = respondedAt;
    }

    public void accept(LocalDateTime respondedAt) {
        this.status = FriendRequestStatus.ACCEPTED;
        this.respondedAt = respondedAt;
    }

    public void reject(LocalDateTime respondedAt) {
        this.status = FriendRequestStatus.REJECTED;
        this.respondedAt = respondedAt;
    }

    public void cancel(LocalDateTime respondedAt) {
        this.status = FriendRequestStatus.CANCELED;
        this.respondedAt = respondedAt;
    }
}
