package com.rolling.api.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final UserRole role;

    public UserPrincipal(Long userId) {
        this(userId, UserRole.USER);
    }

    public UserPrincipal(Long userId, boolean admin) {
        this(userId, admin ? UserRole.ADMIN : UserRole.USER);
    }

    public UserPrincipal(Long userId, UserRole role) {
        this.userId = userId;
        this.role = role == null ? UserRole.USER : role;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMIN;
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
        return true;
    }
}
