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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_devices", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"fcm_token"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDevice extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "fcm_token", nullable = false, length = 512)
    private String fcmToken;

    @Builder
    public UserDevice(User user, String fcmToken) {
        this.fcmToken = fcmToken;
        assignUser(user);
    }

    public void assignUser(User user) {
        if (this.user == user) {
            return;
        }

        if (this.user != null) {
            this.user.unlinkDevice(this);
        }

        this.user = user;

        if (user != null) {
            user.linkDevice(this);
        }
    }
}
