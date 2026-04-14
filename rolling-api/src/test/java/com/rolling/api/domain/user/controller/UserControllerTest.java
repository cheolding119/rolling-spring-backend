package com.rolling.api.domain.user.controller;

import com.rolling.api.domain.user.dto.UserResponse;
import com.rolling.api.domain.user.dto.BlockedUserResponse;
import com.rolling.api.domain.user.dto.UserSettingsResponse;
import com.rolling.api.domain.user.service.UserService;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerTest {

    private MockMvc mockMvc;
    private UserService userService;
    private AdminAccessConfig adminAccessConfig;
    private JwtTokenProvider jwtTokenProvider;
    private UserRepository userRepository;
    private AnnotationConfigWebApplicationContext context;

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

        userService = context.getBean(UserService.class);
        adminAccessConfig = context.getBean(AdminAccessConfig.class);
        jwtTokenProvider = context.getBean(JwtTokenProvider.class);
        userRepository = context.getBean(UserRepository.class);
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
    @DisplayName("내 정보 조회는 설정 정보를 포함한다")
    void getMe_withUserToken_returnsSettings() throws Exception {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);
        given(userService.getMe(2L)).willReturn(userResponse(true));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer user-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.affiliation").value("롤링짐 강남"))
                .andExpect(jsonPath("$.data.settings.pushNotificationEnabled").value(true));
    }

    @Test
    @DisplayName("내 정보 수정은 소속 체육관 필드를 포함해 성공한다")
    void updateMe_withAffiliation_returnsUpdatedProfile() throws Exception {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);
        given(userService.updateMe(eq(2L), any())).willReturn(userResponse(true));

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "affiliation": "롤링짐 강남"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.affiliation").value("롤링짐 강남"));
    }

    @Test
    @DisplayName("사용자 설정 수정은 인증 사용자면 성공한다")
    void updateSettings_withUserToken_returnsUpdatedValue() throws Exception {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);
        given(userService.updateSettings(eq(2L), any())).willReturn(
                UserSettingsResponse.builder()
                        .pushNotificationEnabled(false)
                        .build()
        );

        mockMvc.perform(patch("/api/v1/users/me/settings")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"pushNotificationEnabled\": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.pushNotificationEnabled").value(false));
    }

    @Test
    @DisplayName("사용자 설정 수정에서 pushNotificationEnabled가 없으면 400을 반환한다")
    void updateSettings_withoutPushNotificationEnabled_returnsBadRequest() throws Exception {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);

        mockMvc.perform(patch("/api/v1/users/me/settings")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("사용자 설정 수정은 미인증이면 401을 반환한다")
    void updateSettings_withoutAuthentication_returnsUnauthorized() throws Exception {
        mockMvc.perform(patch("/api/v1/users/me/settings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"pushNotificationEnabled\": false
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("차단한 사용자 목록 조회는 인증 사용자면 성공한다")
    void getBlockedUsers_withUserToken_returnsBlockedUsers() throws Exception {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);
        given(userService.getBlockedUsers(2L)).willReturn(List.of(blockedUserResponse()));

        mockMvc.perform(get("/api/v1/users/blocks")
                        .header("Authorization", "Bearer user-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value(12))
                .andExpect(jsonPath("$.data[0].nickname").value("blocked-user"))
                .andExpect(jsonPath("$.data[0].blockedAt").value("2026-04-14T13:00:00"));
    }

    private UserResponse userResponse(boolean pushNotificationEnabled) {
        return UserResponse.builder()
                .id(2L)
                .nickname("rolling-user")
                .email("user@test.com")
                .phone("010-1234-5678")
                .affiliation("롤링짐 강남")
                .socialProvider("GOOGLE")
                .beltColor("BLUE")
                .createdAt(LocalDateTime.of(2026, 4, 6, 12, 0))
                .withdrawalPending(false)
                .withdrawalScheduledAt(null)
                .isAdmin(false)
                .settings(UserSettingsResponse.builder()
                        .pushNotificationEnabled(pushNotificationEnabled)
                        .build())
                .build();
    }

    private BlockedUserResponse blockedUserResponse() {
        return BlockedUserResponse.builder()
                .userId(12L)
                .nickname("blocked-user")
                .affiliation("blocked-gym")
                .beltColor("BLUE")
                .blockedAt(LocalDateTime.of(2026, 4, 14, 13, 0))
                .build();
    }

    @Configuration
    @EnableWebMvc
    @Import({UserController.class, GlobalExceptionHandler.class, SecurityConfig.class})
    static class TestConfig {

        @Bean
        UserService userService() {
            return mock(UserService.class);
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
