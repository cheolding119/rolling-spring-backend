package com.rolling.api.global.security;

import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.global.logging.LogMdcKeys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class TestUserHeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String TEST_USER_ID_HEADER = "X-Test-User-Id";

    private final UserRepository userRepository;
    private final AdminAccessConfig adminAccessConfig;
    private final Clock clock;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String testUserIdHeader = request.getHeader(TEST_USER_ID_HEADER);

            if (StringUtils.hasText(testUserIdHeader)) {
                authenticateFromHeader(testUserIdHeader);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void authenticateFromHeader(String testUserIdHeader) {
        try {
            Long userId = Long.parseLong(testUserIdHeader);

            Optional<User> userOptional = userRepository.findByIdAndIsWithdrawnFalse(userId);
            UserPrincipal principal;
            if (userOptional.isPresent()) {
                User user = userOptional.get();
                LocalDateTime now = LocalDateTime.now(clock);
                principal = new UserPrincipal(
                        userId,
                        adminAccessConfig.isAdmin(userId),
                        user.getEffectiveAccountStatus(now),
                        user.getEffectiveSuspensionUntil(now)
                );
            } else if (userRepository.existsByIdAndIsWithdrawnFalse(userId)) {
                principal = new UserPrincipal(userId, adminAccessConfig.isAdmin(userId));
            } else {
                log.debug("Local test header authentication rejected for missing or withdrawn user. userId={}", userId);
                return;
            }

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(auth);
            MDC.put(LogMdcKeys.USER_ID, userId.toString());
            log.debug("Local test header authentication succeeded. userId={}", userId);
        } catch (NumberFormatException e) {
            log.debug("Local test header authentication rejected for invalid userId header. value={}", testUserIdHeader);
        }
    }
}
