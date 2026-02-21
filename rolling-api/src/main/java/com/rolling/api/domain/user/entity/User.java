package com.rolling.api.domain.user.entity;

import com.rolling.api.global.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SocialProvider socialProvider;

    @Column(nullable = false)
    private String socialId;

    private String fcmToken;

    @ManyToMany
    @JoinTable(name = "user_blocked_users",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "blocked_user_id"))
    private Set<User> blockedUsers = new HashSet<>();

    @Builder
    public User(String socialId, SocialProvider socialProvider, String nickname, String email, String phone, String fcmToken) {
        this.socialId = socialId;
        this.socialProvider = socialProvider;
        this.nickname = nickname;
        this.email = email;
        this.phone = phone;
        this.fcmToken = fcmToken;
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

    public void updateFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }

    public void blockUser(User user) {
        this.blockedUsers.add(user);
    }

    public void unblockUser(User user) {
        this.blockedUsers.remove(user);
    }
}
