package com.rolling.api.domain.user.entity;

import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"socialId", "socialProvider"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nickname;

    private String email;

    private String phone;

    @Column(length = 255)
    private String affiliation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider socialProvider;

    @Column(nullable = false)
    private String socialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BeltColor beltColor;

    @Column(nullable = false)
    private Boolean pushNotificationEnabled = true;

    @OneToMany(mappedBy = "user")
    private List<UserDevice> devices = new ArrayList<>();

    @Column(nullable = false)
    private Boolean isWithdrawn = false;

    @Column(nullable = false)
    private Boolean withdrawalPending = false;

    private LocalDateTime withdrawalRequestedAt;

    private LocalDateTime withdrawalScheduledAt;

    @ManyToMany
    @JoinTable(name = "user_blocked_users",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "blocked_user_id"))
    private Set<User> blockedUsers = new HashSet<>();

    @Builder
    public User(String socialId,
                SocialProvider socialProvider,
                String nickname,
                String email,
                String phone,
                String affiliation,
                BeltColor beltColor,
                String fcmToken,
                Boolean pushNotificationEnabled) {
        this.socialId = socialId;
        this.socialProvider = socialProvider;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        this.affiliation = affiliation;
        this.beltColor = beltColor == null ? BeltColor.WHITE : beltColor;
        this.pushNotificationEnabled = pushNotificationEnabled == null ? true : pushNotificationEnabled;
        if (fcmToken != null && !fcmToken.isBlank()) {
            new UserDevice(this, fcmToken.trim(), null, null, null);
        }
    }

    public void updateNickname(String nickname) {
        if (nickname != null) {
            this.nickname = nickname;
        }
    }

    public void updateEmail(String email) {
        if (email != null) {
            this.email = email;
        }
    }

    public void updatePhone(String phone) {
        this.phone = phone;
    }

    public void updateAffiliation(String affiliation) {
        if (affiliation != null) {
            this.affiliation = affiliation;
        }
    }

    public void updateBeltColor(BeltColor beltColor) {
        if (beltColor != null) {
            this.beltColor = beltColor;
        }
    }

    public void updatePushNotificationEnabled(boolean enabled) {
        this.pushNotificationEnabled = enabled;
    }

    void linkDevice(UserDevice device) {
        if (!this.devices.contains(device)) {
            this.devices.add(device);
        }
    }

    void unlinkDevice(UserDevice device) {
        this.devices.remove(device);
    }

    public String getFcmToken() {
        if (devices.isEmpty()) {
            return null;
        }
        return devices.get(0).getFcmToken();
    }

    public void blockUser(User user) {
        this.blockedUsers.add(user);
    }

    public void unblockUser(User user) {
        this.blockedUsers.remove(user);
    }

    public void requestWithdrawal(LocalDateTime requestedAt, LocalDateTime scheduledAt) {
        this.withdrawalPending = true;
        this.withdrawalRequestedAt = requestedAt;
        this.withdrawalScheduledAt = scheduledAt;
    }

    public void cancelWithdrawal() {
        this.withdrawalPending = false;
        this.withdrawalRequestedAt = null;
        this.withdrawalScheduledAt = null;
    }

    public void withdraw() {
        this.nickname = "withdrawn_user_" + this.id;
        this.email = null;
        this.phone = null;
        this.affiliation = null;
        this.devices.clear();
        this.socialId = "withdrawn-" + this.id + "-" + UUID.randomUUID();
        this.withdrawalPending = false;
        this.withdrawalRequestedAt = null;
        this.withdrawalScheduledAt = null;
        this.isWithdrawn = true;
        this.blockedUsers.clear();
    }
}
