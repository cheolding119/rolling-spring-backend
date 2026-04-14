package com.rolling.api.domain.user.controller;

import com.rolling.api.domain.user.dto.AdminUserSummaryResponse;
import com.rolling.api.domain.user.dto.UserSanctionResponse;
import com.rolling.api.domain.user.entity.AccountStatus;
import com.rolling.api.domain.user.entity.UserSanctionType;
import com.rolling.api.domain.user.service.UserAdminService;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.config.SecurityConfig;
import com.rolling.api.global.exception.GlobalExceptionHandler;
import com.rolling.api.global.security.AdminAccessConfig;
import com.rolling.api.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.config.SpringDataWebConfiguration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserAdminControllerTest {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;
    private UserAdminService userAdminService;
    private JwtTokenProvider jwtTokenProvider;
    private UserRepository userRepository;
    private AdminAccessConfig adminAccessConfig;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertyValues.of(
                "jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
                "jwt.access-token-expiry=1800000",
                "jwt.refresh-token-expiry=1209600000"
        ).applyTo(context);
        context.register(TestConfig.class);
        context.refresh();

        userAdminService = context.getBean(UserAdminService.class);
        jwtTokenProvider = context.getBean(JwtTokenProvider.class);
        userRepository = context.getBean(UserRepository.class);
        adminAccessConfig = context.getBean(AdminAccessConfig.class);
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("관리자 사용자 목록은 admin accessToken이면 성공한다")
    void list_withAdminToken_returnsOk() throws Exception {
        given(jwtTokenProvider.validateToken("admin-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("admin-token")).willReturn(1L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(1L)).willReturn(true);
        given(adminAccessConfig.isAdmin(1L)).willReturn(true);
        given(userAdminService.findUsers(eq("suspended"), eq(AccountStatus.SUSPENDED), any()))
                .willReturn(new PageImpl<>(
                        List.of(AdminUserSummaryResponse.builder()
                                .id(10L)
                                .nickname("rolling-user")
                                .email("user@test.com")
                                .affiliation("롤링짐 강남")
                                .createdAt(LocalDateTime.of(2026, 4, 10, 12, 0))
                                .accountStatus(AccountStatus.SUSPENDED)
                                .suspensionUntil(LocalDateTime.of(2026, 4, 20, 0, 0))
                                .lastSanctionAt(LocalDateTime.of(2026, 4, 14, 13, 0))
                                .build()),
                        PageRequest.of(0, 20, Sort.by("createdAt").descending()),
                        1
                ));

        mockMvc.perform(get("/api/v1/admin/users")
                        .param("q", "suspended")
                        .param("status", "SUSPENDED")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(10))
                .andExpect(jsonPath("$.data.content[0].accountStatus").value("SUSPENDED"));
    }

    @Test
    @DisplayName("관리자 사용자 제재 생성은 admin accessToken이면 성공한다")
    void createSanction_withAdminToken_returnsOk() throws Exception {
        given(jwtTokenProvider.validateToken("admin-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("admin-token")).willReturn(1L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(1L)).willReturn(true);
        given(adminAccessConfig.isAdmin(1L)).willReturn(true);
        given(userAdminService.createSanction(anyLong(), eq(10L), any()))
                .willReturn(UserSanctionResponse.builder()
                        .id(100L)
                        .type(UserSanctionType.TEMP_SUSPEND)
                        .reason("반복적인 욕설")
                        .startsAt(LocalDateTime.of(2026, 4, 14, 13, 0))
                        .endsAt(LocalDateTime.of(2026, 4, 20, 0, 0))
                        .createdByUserId(1L)
                        .createdAt(LocalDateTime.of(2026, 4, 14, 13, 0))
                        .build());

        mockMvc.perform(post("/api/v1/admin/users/10/sanctions")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "TEMP_SUSPEND",
                                  "reason": "반복적인 욕설",
                                  "memo": "운영 규정 위반",
                                  "endsAt": "2026-04-20T00:00:00"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(100))
                .andExpect(jsonPath("$.data.type").value("TEMP_SUSPEND"));
    }

    @Test
    @DisplayName("관리자 사용자 운영 API는 일반 사용자 accessToken이면 403을 반환한다")
    void list_withUserToken_returnsForbidden() throws Exception {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);

        mockMvc.perform(get("/api/v1/admin/users")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Configuration
    @EnableWebMvc
    @Import({UserAdminController.class, GlobalExceptionHandler.class, SecurityConfig.class, SpringDataWebConfiguration.class})
    static class TestConfig {

        @Bean
        UserAdminService userAdminService() {
            return mock(UserAdminService.class);
        }

        @Bean
        JwtTokenProvider jwtTokenProvider() {
            return mock(JwtTokenProvider.class);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }

        @Bean
        AdminAccessConfig adminAccessConfig() {
            return mock(AdminAccessConfig.class);
        }
    }
}
