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
import java.util.stream.Collectors;
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

    @Column(length = 50)
    private String communityNickname;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccountStatus accountStatus = AccountStatus.ACTIVE;

    private LocalDateTime suspensionUntil;

    @Column(length = 255)
    private String sanctionReasonSummary;

    @Column(nullable = false)
    private Boolean pushNotificationEnabled = true;

    @Column(nullable = false)
    private Boolean showOwnReactions = true;

    @OneToMany(mappedBy = "user")
    private List<UserDevice> devices = new ArrayList<>();

    @Column(nullable = false)
    private Boolean isWithdrawn = false;

    @Column(nullable = false)
    private Boolean withdrawalPending = false;

    private LocalDateTime withdrawalRequestedAt;

    private LocalDateTime withdrawalScheduledAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserBlock> blockedUserLinks = new HashSet<>();

    @Builder
    public User(String socialId,
                SocialProvider socialProvider,
                String nickname,
                String email,
                String phone,
                String affiliation,
                BeltColor beltColor,
                String fcmToken,
                Boolean pushNotificationEnabled,
                Boolean showOwnReactions) {
        this.socialId = socialId;
        this.socialProvider = socialProvider;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        this.affiliation = affiliation;
        this.beltColor = beltColor == null ? BeltColor.WHITE : beltColor;
        this.accountStatus = AccountStatus.ACTIVE;
        this.pushNotificationEnabled = pushNotificationEnabled == null ? true : pushNotificationEnabled;
        this.showOwnReactions = showOwnReactions == null ? true : showOwnReactions;
        if (fcmToken != null && !fcmToken.isBlank()) {
            new UserDevice(this, fcmToken.trim(), null, null, null);
        }
    }

    public void updateNickname(String nickname) {
        if (nickname != null) {
            this.nickname = nickname;
        }
    }

    public void updateCommunityNickname(String communityNickname) {
        if (communityNickname != null) {
            this.communityNickname = communityNickname;
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

    public void updateShowOwnReactions(boolean showOwnReactions) {
        this.showOwnReactions = showOwnReactions;
    }

    public AccountStatus getEffectiveAccountStatus(LocalDateTime now) {
        if (accountStatus == AccountStatus.BANNED) {
            return AccountStatus.SUSPENDED;
        }
        if (accountStatus == AccountStatus.SUSPENDED && suspensionUntil != null && !now.isBefore(suspensionUntil)) {
            return AccountStatus.ACTIVE;
        }
        return accountStatus == null ? AccountStatus.ACTIVE : accountStatus;
    }

    public LocalDateTime getEffectiveSuspensionUntil(LocalDateTime now) {
        if (getEffectiveAccountStatus(now) != AccountStatus.SUSPENDED) {
            return null;
        }
        return suspensionUntil;
    }

    public String getEffectiveSanctionReasonSummary(LocalDateTime now) {
        AccountStatus effectiveStatus = getEffectiveAccountStatus(now);
        if (effectiveStatus == AccountStatus.ACTIVE || effectiveStatus == AccountStatus.WITHDRAWN) {
            return null;
        }
        return sanctionReasonSummary;
    }

    public boolean isTemporarilySuspended(LocalDateTime now) {
        return getEffectiveAccountStatus(now) == AccountStatus.SUSPENDED;
    }

    public void warn(String reasonSummary) {
        this.accountStatus = AccountStatus.WARNING;
        this.suspensionUntil = null;
        this.sanctionReasonSummary = normalizeSummary(reasonSummary);
    }

    public void suspend(LocalDateTime until, String reasonSummary) {
        this.accountStatus = AccountStatus.SUSPENDED;
        this.suspensionUntil = until;
        this.sanctionReasonSummary = normalizeSummary(reasonSummary);
    }

    @Deprecated(forRemoval = false)
    public void ban(String reasonSummary) {
        this.accountStatus = AccountStatus.BANNED;
        this.suspensionUntil = null;
        this.sanctionReasonSummary = normalizeSummary(reasonSummary);
    }

    public void releaseSanction() {
        this.accountStatus = AccountStatus.ACTIVE;
        this.suspensionUntil = null;
        this.sanctionReasonSummary = null;
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
        if (user == null || isBlocked(user)) {
            return;
        }
        this.blockedUserLinks.add(new UserBlock(this, user, LocalDateTime.now()));
    }

    public void unblockUser(User user) {
        this.blockedUserLinks.removeIf(link -> link.isBlockedUser(user));
    }

    public Set<User> getBlockedUsers() {
        return blockedUserLinks.stream()
                .map(UserBlock::getBlockedUser)
                .collect(Collectors.toSet());
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
        this.communityNickname = this.nickname;
        this.email = null;
        this.phone = null;
        this.affiliation = null;
        this.devices.clear();
        this.socialId = "withdrawn-" + this.id + "-" + UUID.randomUUID();
        this.withdrawalPending = false;
        this.withdrawalRequestedAt = null;
        this.withdrawalScheduledAt = null;
        this.isWithdrawn = true;
        this.accountStatus = AccountStatus.WITHDRAWN;
        this.suspensionUntil = null;
        this.sanctionReasonSummary = null;
        this.blockedUserLinks.clear();
    }

    private boolean isBlocked(User user) {
        return blockedUserLinks.stream().anyMatch(link -> link.isBlockedUser(user));
    }

    private String normalizeSummary(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
