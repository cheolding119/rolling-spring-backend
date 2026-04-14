package com.rolling.api.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "user_blocked_users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class UserBlock {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private UserBlockId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("blockedUserId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocked_user_id", nullable = false)
    private User blockedUser;

    @Column(name = "blocked_at", nullable = false)
    private LocalDateTime blockedAt;

    public UserBlock(User user, User blockedUser, LocalDateTime blockedAt) {
        this.user = Objects.requireNonNull(user, "user");
        this.blockedUser = Objects.requireNonNull(blockedUser, "blockedUser");
        this.blockedAt = Objects.requireNonNull(blockedAt, "blockedAt");
        this.id = new UserBlockId(user.getId(), blockedUser.getId());
    }

    public boolean isBlockedUser(User candidate) {
        return candidate != null && blockedUser != null && Objects.equals(blockedUser.getId(), candidate.getId());
    }
}
