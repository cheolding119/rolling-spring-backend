package com.rolling.api.global.config;

import com.rolling.api.domain.openmat.config.OpenMatTestingAccessConfig;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.security.AdminAccessConfig;
import com.rolling.api.global.security.jwt.JwtAuthenticationFilter;
import com.rolling.api.global.security.jwt.JwtTokenProvider;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
    private final OpenMatTestingAccessConfig openMatTestingAccessConfig;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // 인증 불필요 엔드포인트
                    auth.requestMatchers(
                            "/api/v1/auth/login",
                            "/api/v1/auth/refresh",
                            "/swagger-ui/**",
                            "/swagger-ui.html",
                            "/v3/api-docs/**",
                            "/error"
                    ).permitAll();

                    // 수동 대회 크롤링은 관리자만 실행 가능
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/tournaments/crawl", "/api/v1/tournaments/crawl/**").hasRole("INTERNAL_API");

                    // 내 신청 목록은 인증 필요 (/{id} 공개 조회 규칙과 충돌 방지)
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/open-mats/my").authenticated();
                    // 오픈매트 조회는 비로그인 허용
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/open-mats", "/api/v1/open-mats/{id}").permitAll();
                    if (openMatTestingAccessConfig.isAllowUnauthenticatedUpdate()) {
                        auth.requestMatchers(HttpMethod.PUT, "/api/v1/open-mats/{id}").permitAll();
                        auth.requestMatchers(HttpMethod.DELETE, "/api/v1/open-mats/{id}").permitAll();
                    }
                    // 대회 조회는 비로그인 허용
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/tournaments", "/api/v1/tournaments/{id}").permitAll();
                    // 공지사항 조회는 비로그인 허용
                    auth.requestMatchers(HttpMethod.GET, "/api/v1/notices", "/api/v1/notices/{id}").permitAll();
                    // 공지사항 운영 API는 관리자만 실행 가능
                    auth.requestMatchers(HttpMethod.POST, "/api/v1/notices", "/api/v1/notices/**").hasRole("INTERNAL_API");
                    auth.requestMatchers(HttpMethod.PUT, "/api/v1/notices", "/api/v1/notices/**").hasRole("INTERNAL_API");
                    auth.requestMatchers(HttpMethod.DELETE, "/api/v1/notices", "/api/v1/notices/**").hasRole("INTERNAL_API");
                    // 나머지는 모두 인증 필요
                    auth.anyRequest().authenticated();
                })
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, e) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json;charset=UTF-8");
                            response.getWriter().write(
                                    "{\"success\":false,\"error\":{\"code\":\"UNAUTHORIZED\",\"message\":\"인증이 필요합니다\"}}"
                            );
                        })
                )
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider, userRepository, adminAccessConfig),
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of("*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
