package com.rolling.api.global.security.jwt;

import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.logging.LogMdcKeys;
import com.rolling.api.global.security.AdminAccessConfig;
import com.rolling.api.global.security.UserPrincipal;
import com.rolling.api.domain.user.entity.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AdminAccessConfig adminAccessConfig;
    private final Clock clock;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = resolveToken(request);

        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            Optional<User> userOptional = userRepository.findByIdAndIsWithdrawnFalse(userId);
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                LocalDateTime now = LocalDateTime.now(clock);
                UserPrincipal principal = new UserPrincipal(
                        userId,
                        adminAccessConfig.isAdmin(userId),
                        user.getEffectiveAccountStatus(now),
                        user.getEffectiveSuspensionUntil(now)
                );
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                MDC.put(LogMdcKeys.USER_ID, userId.toString());
                log.debug("JWT authentication succeeded. userId={}", userId);
            } else if (userRepository.existsByIdAndIsWithdrawnFalse(userId)) {
                UserPrincipal principal = new UserPrincipal(
                        userId,
                        adminAccessConfig.isAdmin(userId)
                );
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(auth);
                MDC.put(LogMdcKeys.USER_ID, userId.toString());
                log.debug("JWT authentication succeeded with legacy existence check. userId={}", userId);
            } else {
                log.debug("JWT authentication rejected for missing or withdrawn user. userId={}", userId);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
