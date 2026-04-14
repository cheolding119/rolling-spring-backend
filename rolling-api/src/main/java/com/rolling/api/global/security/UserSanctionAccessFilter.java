package com.rolling.api.global.security;

import com.rolling.api.global.logging.LogMdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
public class UserSanctionAccessFilter extends OncePerRequestFilter {

    private static final List<RouteRule> TEMP_SUSPEND_ALLOWED_ROUTES = List.of(
            new RouteRule("POST", "/api/v1/auth/logout"),
            new RouteRule("POST", "/api/v1/auth/refresh"),
            new RouteRule("GET", "/api/v1/users/me"),
            new RouteRule("PATCH", "/api/v1/users/me/settings"),
            new RouteRule("GET", "/api/v1/users/blocks"),
            new RouteRule("POST", "/api/v1/users/{id}/block"),
            new RouteRule("DELETE", "/api/v1/users/{id}/block"),
            new RouteRule("GET", "/api/v1/inquiries"),
            new RouteRule("POST", "/api/v1/inquiries"),
            new RouteRule("GET", "/api/v1/inquiries/{id}"),
            new RouteRule("GET", "/api/v1/notices"),
            new RouteRule("GET", "/api/v1/notices/{id}")
    );

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return true;
        }
        if (principal.isAdmin()) {
            return false;
        }
        return !principal.isSuspended() && !principal.isBanned();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            filterChain.doFilter(request, response);
            return;
        }

        if (principal.isBanned()) {
            if (isLogoutRequest(request)) {
                filterChain.doFilter(request, response);
                return;
            }
            writeForbiddenResponse(response, "이용이 정지된 계정입니다");
            return;
        }

        if (principal.isSuspended() && !isAllowedDuringTemporarySuspension(request)) {
            writeForbiddenResponse(response, "일시정지된 계정은 제한된 기능만 사용할 수 있습니다");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAllowedDuringTemporarySuspension(HttpServletRequest request) {
        String method = request.getMethod();
        String path = request.getRequestURI();
        return TEMP_SUSPEND_ALLOWED_ROUTES.stream()
                .anyMatch(route -> route.matches(method, path, pathMatcher));
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        return "POST".equalsIgnoreCase(request.getMethod())
                && "/api/v1/auth/logout".equals(request.getRequestURI());
    }

    private void writeForbiddenResponse(HttpServletResponse response, String message) throws IOException {
        MDC.put(LogMdcKeys.STATUS, Integer.toString(HttpStatus.FORBIDDEN.value()));
        MDC.put(LogMdcKeys.ERROR_CODE, "FORBIDDEN");
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"error\":{\"code\":\"FORBIDDEN\",\"message\":\"" + message + "\"}}"
        );
    }

    private record RouteRule(String method, String pathPattern) {
        private boolean matches(String requestMethod, String requestPath, AntPathMatcher pathMatcher) {
            return method.equalsIgnoreCase(requestMethod) && pathMatcher.match(pathPattern, requestPath);
        }
    }
}
