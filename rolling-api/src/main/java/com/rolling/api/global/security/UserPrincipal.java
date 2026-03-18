package com.rolling.api.global.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

@Getter
public class UserPrincipal implements UserDetails {

    private final Long userId;
    private final boolean admin;
    private final boolean internalApi;

    public static UserPrincipal systemAdmin() {
        return new UserPrincipal(-1L, false, true);
    }

    public UserPrincipal(Long userId) {
        this(userId, false, false);
    }

    public UserPrincipal(Long userId, boolean admin) {
        this(userId, admin, false);
    }

    public UserPrincipal(Long userId, boolean admin, boolean internalApi) {
        this.userId = userId;
        this.admin = admin;
        this.internalApi = internalApi;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        if (internalApi) {
            return List.of(new SimpleGrantedAuthority("ROLE_INTERNAL_API"));
        }
        if (admin) {
            return List.of(
                    new SimpleGrantedAuthority("ROLE_USER"),
                    new SimpleGrantedAuthority("ROLE_ADMIN")
            );
        }
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
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
