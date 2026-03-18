package com.rolling.api.global.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalTest {

    @Test
    @DisplayName("관리자 사용자는 ROLE_USER와 ROLE_ADMIN 권한을 가진다")
    void adminPrincipalHasAdminAuthority() {
        UserPrincipal principal = new UserPrincipal(1L, true);

        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    @Test
    @DisplayName("일반 사용자는 ROLE_USER 권한만 가진다")
    void normalPrincipalHasUserAuthorityOnly() {
        UserPrincipal principal = new UserPrincipal(1L, false);

        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("운영 admin key 인증 사용자는 ROLE_INTERNAL_API 권한만 가진다")
    void systemAdminHasInternalApiAuthorityOnly() {
        UserPrincipal principal = UserPrincipal.systemAdmin();

        assertThat(principal.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_INTERNAL_API");
    }
}
