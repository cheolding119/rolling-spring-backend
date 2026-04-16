package com.rolling.api.global.security;

import com.rolling.api.domain.user.entity.AccountStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class UserSanctionAccessFilterTest {

    private final UserSanctionAccessFilter filter = new UserSanctionAccessFilter();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("일시정지 사용자는 허용된 제한 모드 경로에 접근할 수 있다")
    void suspendedUser_canAccessAllowedRoute() throws Exception {
        UserPrincipal principal = new UserPrincipal(2L, false, AccountStatus.SUSPENDED, LocalDateTime.of(2026, 4, 20, 0, 0));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/inquiries");
        MockHttpServletResponse response = new MockHttpServletResponse();
        var chain = mock(jakarta.servlet.FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("일시정지 사용자는 허용되지 않은 경로에서 차단된다")
    void suspendedUser_isBlockedOutsideAllowedRoutes() throws Exception {
        UserPrincipal principal = new UserPrincipal(2L, false, AccountStatus.SUSPENDED, LocalDateTime.of(2026, 4, 20, 0, 0));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/open-mats/my");
        MockHttpServletResponse response = new MockHttpServletResponse();
        var chain = mock(jakarta.servlet.FilterChain.class);

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(chain);
    }

    @Test
    @DisplayName("일시정지 사용자는 회원 탈퇴 요청 경로에 접근할 수 있다")
    void suspendedUser_canAccessWithdrawRoute() throws Exception {
        UserPrincipal principal = new UserPrincipal(2L, false, AccountStatus.SUSPENDED, LocalDateTime.of(2126, 4, 20, 0, 0));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );

        MockHttpServletRequest request = new MockHttpServletRequest("DELETE", "/api/v1/auth/withdraw");
        MockHttpServletResponse response = new MockHttpServletResponse();
        var chain = mock(jakarta.servlet.FilterChain.class);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }
}
