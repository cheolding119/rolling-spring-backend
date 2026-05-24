package com.rolling.api.domain.traininglog.entity;

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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "user_training_log_share_settings",
        indexes = {
                @Index(name = "idx_user_training_log_share_settings_user_id", columnList = "user_id", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserTrainingLogShareSetting extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "share_with_friends", nullable = false)
    private boolean shareWithFriends;

    @Builder
    public UserTrainingLogShareSetting(User user, boolean shareWithFriends) {
        this.user = user;
        this.shareWithFriends = shareWithFriends;
    }

    public void updateShareWithFriends(boolean shareWithFriends) {
        this.shareWithFriends = shareWithFriends;
    }
}
