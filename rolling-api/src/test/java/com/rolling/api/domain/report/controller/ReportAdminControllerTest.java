package com.rolling.api.domain.report.controller;

import com.rolling.api.domain.report.dto.ReportResponse;
import com.rolling.api.domain.report.dto.ReportTargetSummary;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.report.entity.ReportTargetType;
import com.rolling.api.domain.report.service.ReportService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportAdminControllerTest {

    private MockMvc mockMvc;
    private ReportService reportService;
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

        reportService = context.getBean(ReportService.class);
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
    @DisplayName("관리자 신고 목록 조회는 admin accessToken이면 성공한다")
    void list_withAdminToken_returnsOk() throws Exception {
        given(jwtTokenProvider.validateToken("admin-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("admin-token")).willReturn(1L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(1L)).willReturn(true);
        given(adminAccessConfig.isAdmin(1L)).willReturn(true);
        given(reportService.findAllForAdmin(any(), any(), any(), any(), any()))
                .willReturn(new PageImpl<>(List.of(reportResponse(7L, ReportStatus.RECEIVED)), org.springframework.data.domain.PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/v1/admin/reports")
                        .param("status", "RECEIVED")
                        .param("targetType", "OPEN_MAT")
                        .param("createdFrom", "2026-03-01")
                        .param("createdTo", "2026-03-31")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(7))
                .andExpect(jsonPath("$.data.content[0].targetSummary.totalReportCount").value(4));

        verify(reportService).findAllForAdmin(
                eq(ReportStatus.RECEIVED),
                eq(ReportTargetType.OPEN_MAT),
                eq(java.time.LocalDate.of(2026, 3, 1)),
                eq(java.time.LocalDate.of(2026, 3, 31)),
                any()
        );
    }

    @Test
    @DisplayName("관리자 신고 상태 변경은 admin accessToken이면 성공한다")
    void updateStatus_withAdminToken_returnsOk() throws Exception {
        given(jwtTokenProvider.validateToken("admin-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("admin-token")).willReturn(1L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(1L)).willReturn(true);
        given(adminAccessConfig.isAdmin(1L)).willReturn(true);
        given(reportService.updateStatus(any(), any(), any())).willReturn(reportResponse(7L, ReportStatus.RESOLVED));

        mockMvc.perform(patch("/api/v1/admin/reports/7/status")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"status\": \"RESOLVED\",
                                  \"processingMemo\": \"반복 광고성 내용으로 확인되어 숨김 처리했습니다.\",
                                  \"finalAction\": \"CONTENT_HIDDEN\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("RESOLVED"))
                .andExpect(jsonPath("$.data.finalAction").value("CONTENT_HIDDEN"));
    }

    @Test
    @DisplayName("관리자 신고 운영 API는 일반 사용자 accessToken이면 403을 반환한다")
    void list_withUserToken_returnsForbidden() throws Exception {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);

        mockMvc.perform(get("/api/v1/admin/reports")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isForbidden());
    }

    private ReportResponse reportResponse(Long id, ReportStatus status) {
        return ReportResponse.builder()
                .id(id)
                .reporterUserId(2L)
                .reporterNickname("rolling-user")
                .targetType(ReportTargetType.OPEN_MAT)
                .targetId(10L)
                .reason(ReportReason.SPAM)
                .status(status)
                .processedByUserId(status == ReportStatus.RECEIVED ? null : 1L)
                .processedAt(status == ReportStatus.RECEIVED ? null : LocalDateTime.of(2026, 3, 20, 12, 0))
                .processingMemo(status == ReportStatus.RECEIVED ? null : "반복 광고성 내용으로 확인되어 숨김 처리했습니다.")
                .finalAction(status == ReportStatus.RECEIVED ? null : "CONTENT_HIDDEN")
                .targetSummary(ReportTargetSummary.builder()
                        .targetType(ReportTargetType.OPEN_MAT)
                        .targetId(10L)
                        .totalReportCount(4)
                        .receivedCount(1)
                        .inReviewCount(1)
                        .resolvedCount(2)
                        .rejectedCount(0)
                        .build())
                .createdAt(LocalDateTime.of(2026, 3, 20, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 20, 12, 0))
                .build();
    }

    @Configuration
    @EnableWebMvc
    @Import({ReportAdminController.class, GlobalExceptionHandler.class, SecurityConfig.class, SpringDataWebConfiguration.class})
    static class TestConfig {

        @Bean
        ReportService reportService() {
            return mock(ReportService.class);
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
