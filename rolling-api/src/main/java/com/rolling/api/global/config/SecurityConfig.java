package com.rolling.api.global.config;

import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.logging.LogMdcKeys;
import com.rolling.api.global.logging.RequestTrackingFilter;
import com.rolling.api.global.security.AdminAccessConfig;
import com.rolling.api.global.security.UserSanctionAccessFilter;
import com.rolling.api.global.security.TestUserHeaderAuthenticationFilter;
import com.rolling.api.global.security.jwt.JwtAuthenticationFilter;
import com.rolling.api.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.time.Clock;
import java.time.ZoneId;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AdminAccessConfig adminAccessConfig;
    private final ObjectProvider<TestUserHeaderAuthenticationFilter> testUserHeaderAuthenticationFilterProvider;
    private final ObjectProvider<Clock> clockProvider;

    @Bean
    @Order(0)
    public SecurityFilterChain actuatorSecurityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtTokenProvider, userRepository, adminAccessConfig, resolveClock());
        UserSanctionAccessFilter userSanctionAccessFilter = new UserSanctionAccessFilter();

        http
                .securityMatcher("/actuator/**")
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/actuator/prometheus"
                        ).permitAll()
                        .anyRequest().hasRole("ADMIN"))
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> writeUnauthorizedResponse(response)))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        http.addFilterAfter(userSanctionAccessFilter, JwtAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtTokenProvider, userRepository, adminAccessConfig, resolveClock());
        RequestTrackingFilter requestTrackingFilter = new RequestTrackingFilter();
        UserSanctionAccessFilter userSanctionAccessFilter = new UserSanctionAccessFilter();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/error"
                    ).permitAll();

                    auth.requestMatchers(HttpMethod.POST, "/api/v1/tournaments/crawl", "/api/v1/tournaments/crawl/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/admin/inquiries", "/api/v1/admin/inquiries/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/admin/reports", "/api/v1/admin/reports/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/admin/users", "/api/v1/admin/users/**").hasRole("ADMIN");

                    auth.requestMatchers(HttpMethod.GET, "/api/v1/open-mats/my", "/api/v1/open-mats/my-hosting").authenticated();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/open-mats", "/api/v1/open-mats/{id}").permitAll();
                    auth.requestMatchers("/api/v1/admin/open-mats", "/api/v1/admin/open-mats/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/tournaments", "/api/v1/tournaments/{id}").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/notices", "/api/v1/notices/{id}").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/notices", "/api/v1/notices/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PUT, "/api/v1/notices", "/api/v1/notices/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.DELETE, "/api/v1/notices", "/api/v1/notices/**").hasRole("ADMIN");
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> writeUnauthorizedResponse(response))
                )
                .addFilterBefore(requestTrackingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, RequestTrackingFilter.class);

        TestUserHeaderAuthenticationFilter testUserHeaderAuthenticationFilter =
                testUserHeaderAuthenticationFilterProvider.getIfAvailable();
        if (testUserHeaderAuthenticationFilter != null) {
            http.addFilterAfter(testUserHeaderAuthenticationFilter, JwtAuthenticationFilter.class);
            http.addFilterAfter(userSanctionAccessFilter, TestUserHeaderAuthenticationFilter.class);
        } else {
            http.addFilterAfter(userSanctionAccessFilter, JwtAuthenticationFilter.class);
        }

        return http.build();
    }

    private void writeUnauthorizedResponse(HttpServletResponse response) throws java.io.IOException {
        MDC.put(LogMdcKeys.ERROR_CODE, "UNAUTHORIZED");
        MDC.put(LogMdcKeys.STATUS, Integer.toString(HttpServletResponse.SC_UNAUTHORIZED));

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\"}}"
        );
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "https://admin.rolling-app.com",
                "http://localhost:5173",
                "http://127.0.0.1:5173"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                RequestTrackingFilter.REQUEST_ID_HEADER,
                RequestTrackingFilter.TRACE_ID_HEADER,
                "X-Correlation-Id",
                "traceparent"
        ));
        configuration.setExposedHeaders(List.of(RequestTrackingFilter.REQUEST_ID_HEADER, RequestTrackingFilter.TRACE_ID_HEADER));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private Clock resolveClock() {
        Clock clock = clockProvider.getIfAvailable();
        return clock != null ? clock : Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
