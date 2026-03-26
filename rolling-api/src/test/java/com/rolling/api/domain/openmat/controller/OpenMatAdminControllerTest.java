package com.rolling.api.domain.openmat.controller;

import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.openmat.service.OpenMatService;
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
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenMatAdminControllerTest {

    private MockMvc mockMvc;
    private OpenMatService openMatService;
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

        openMatService = context.getBean(OpenMatService.class);
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
    @DisplayName("관리자는 신고 차단 해제 API를 호출할 수 있다")
    void clearReportBlock_withAdminToken_returnsOk() throws Exception {
        OpenMatResponse response = OpenMatResponse.builder()
                .id(22L)
                .title("테스트 오픈매트")
                .status(OpenMatStatus.RECRUITING)
                .reported(false)
                .region(Region.SEOUL)
                .currentParticipants(0)
                .startDateTime(LocalDateTime.of(2026, 3, 12, 19, 0))
                .endDateTime(LocalDateTime.of(2026, 3, 12, 21, 0))
                .hostId(1L)
                .hostNickname("host")
                .createdAt(LocalDateTime.of(2026, 3, 10, 12, 0))
                .build();

        given(jwtTokenProvider.validateToken("admin-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("admin-token")).willReturn(1L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(1L)).willReturn(true);
        given(adminAccessConfig.isAdmin(1L)).willReturn(true);
        given(openMatService.clearReportBlock(anyLong(), anyLong())).willReturn(response);

        mockMvc.perform(patch("/api/v1/admin/open-mats/22/report-block")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(22))
                .andExpect(jsonPath("$.data.reported").value(false));

        verify(openMatService).clearReportBlock(1L, 22L);
    }

    @Test
    @DisplayName("관리자 오픈매트 운영 API는 일반 사용자 accessToken이면 403을 반환한다")
    void clearReportBlock_withUserToken_returnsForbidden() throws Exception {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);

        mockMvc.perform(patch("/api/v1/admin/open-mats/22/report-block")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    @Configuration
    @EnableWebMvc
    @Import({OpenMatAdminController.class, GlobalExceptionHandler.class, SecurityConfig.class})
    static class TestConfig {

        @Bean
        OpenMatService openMatService() {
            return mock(OpenMatService.class);
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