package com.rolling.api.global.config;

import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.logging.LogMdcKeys;
import com.rolling.api.global.logging.RequestTrackingFilter;
import com.rolling.api.global.security.AdminAccessConfig;
import com.rolling.api.global.security.jwt.JwtAuthenticationFilter;
import com.rolling.api.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AdminAccessConfig adminAccessConfig;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationFilter jwtAuthenticationFilter =
                new JwtAuthenticationFilter(jwtTokenProvider, userRepository, adminAccessConfig);
        RequestTrackingFilter requestTrackingFilter = new RequestTrackingFilter();

        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh",
                            "/actuator/health",
                            "/actuator/health/**",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/error"
                    ).permitAll();

                    auth.requestMatchers("/actuator/**").hasRole("ADMIN");

                    auth.requestMatchers(HttpMethod.POST, "/api/v1/tournaments/crawl", "/api/v1/tournaments/crawl/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/admin/inquiries", "/api/v1/admin/inquiries/**").hasRole("ADMIN");
                    auth.requestMatchers("/api/v1/admin/reports", "/api/v1/admin/reports/**").hasRole("ADMIN");

                    auth.requestMatchers(HttpMethod.GET, "/api/v1/open-mats/my", "/api/v1/open-mats/my-hosting").authenticated();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/open-mats", "/api/v1/open-mats/{id}").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/tournaments", "/api/v1/tournaments/{id}").permitAll();
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/notices", "/api/v1/notices/{id}").permitAll();
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/notices", "/api/v1/notices/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.PUT, "/api/v1/notices", "/api/v1/notices/**").hasRole("ADMIN");
                    auth.requestMatchers(HttpMethod.DELETE, "/api/v1/notices", "/api/v1/notices/**").hasRole("ADMIN");
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            MDC.put(LogMdcKeys.ERROR_CODE, "UNAUTHORIZED");
                            MDC.put(LogMdcKeys.STATUS, Integer.toString(HttpServletResponse.SC_UNAUTHORIZED));

                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\"}}"
                            );
                        })
                )
                .addFilterBefore(requestTrackingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(jwtAuthenticationFilter, RequestTrackingFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of(RequestTrackingFilter.REQUEST_ID_HEADER, RequestTrackingFilter.TRACE_ID_HEADER));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
