package com.rolling.api.global.security;

import com.rolling.api.domain.user.entity.AccountStatus;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.time.LocalDateTime;
import java.util.stream.Stream;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final UserRole role;
    private final AccountStatus accountStatus;
    private final LocalDateTime suspensionUntil;

    public UserPrincipal(Long userId) {
        this(userId, UserRole.USER, AccountStatus.ACTIVE, null);
    }

    public UserPrincipal(Long userId, boolean admin) {
        this(userId, admin ? UserRole.ADMIN : UserRole.USER, AccountStatus.ACTIVE, null);
    }

    public UserPrincipal(Long userId, UserRole role) {
        this(userId, role, AccountStatus.ACTIVE, null);
    }

    public UserPrincipal(Long userId, boolean admin, AccountStatus accountStatus, LocalDateTime suspensionUntil) {
        this(userId, admin ? UserRole.ADMIN : UserRole.USER, accountStatus, suspensionUntil);
    }

    public UserPrincipal(Long userId, UserRole role, AccountStatus accountStatus, LocalDateTime suspensionUntil) {
        this.userId = userId;
        this.role = role == null ? UserRole.USER : role;
        this.accountStatus = accountStatus == null ? AccountStatus.ACTIVE : accountStatus;
        this.suspensionUntil = suspensionUntil;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isSuspended() {
        return accountStatus == AccountStatus.SUSPENDED;
    }

    public boolean isWithdrawn() {
        return accountStatus == AccountStatus.WITHDRAWN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Stream.of(UserRole.USER, role == UserRole.ADMIN ? UserRole.ADMIN : null)
                .filter(java.util.Objects::nonNull)
                .map(value -> new SimpleGrantedAuthority("ROLE_" + value.name()))
                .toList();
    }

    @Override
    public String getPassword() {
        return null;
    }

    @Override
    public String getUsername() {
        return String.valueOf(userId);
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return !isWithdrawn();
    }
}
