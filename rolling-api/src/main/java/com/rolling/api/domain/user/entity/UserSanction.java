package com.rolling.api.domain.user.entity;

import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "user_sanctions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserSanction extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "sanction_type", nullable = false)
    private String sanctionType;

    @Column(nullable = false, length = 255)
    private String reason;

    @Column(length = 1000)
    private String memo;

    @Column(name = "starts_at", nullable = false)
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Column(name = "released_by_user_id")
    private Long releasedByUserId;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Builder
    public UserSanction(User user,
                        UserSanctionType sanctionType,
                        String reason,
                        String memo,
                        LocalDateTime startsAt,
                        LocalDateTime endsAt,
                        Long createdByUserId) {
        this.user = user;
        this.sanctionType = sanctionType == null ? null : sanctionType.name();
        this.reason = reason;
        this.memo = memo;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
        this.createdByUserId = createdByUserId;
    }

    public UserSanctionType getSanctionTypeEnum() {
        return sanctionType == null ? null : UserSanctionType.valueOf(sanctionType);
    }

    public UserSanctionType getPolicySanctionTypeEnum() {
        UserSanctionType type = getSanctionTypeEnum();
        if (type == UserSanctionType.PERMANENT_BAN) {
            return UserSanctionType.TEMP_SUSPEND;
        }
        return type;
    }

    public boolean isReleased() {
        return releasedAt != null;
    }

    public void release(Long releasedByUserId, LocalDateTime releasedAt) {
        this.releasedByUserId = releasedByUserId;
        this.releasedAt = releasedAt;
    }
}
