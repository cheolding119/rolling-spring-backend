package com.rolling.api.domain.openmat.controller;

import com.rolling.api.domain.openmat.dto.OpenMatCommentResponse;
import com.rolling.api.domain.openmat.service.OpenMatCommentService;
import com.rolling.api.domain.openmat.service.OpenMatImageUploadService;
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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OpenMatCommentSecurityTest {

    private MockMvc mockMvc;
    private OpenMatCommentService openMatCommentService;
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

        openMatCommentService = context.getBean(OpenMatCommentService.class);
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
    @DisplayName("오픈매트 댓글 목록 조회는 비회원에게도 허용된다")
    void comments_withoutToken_returnsSuccess() throws Exception {
        given(openMatCommentService.findComments(null, false, 10L)).willReturn(List.of(
                OpenMatCommentResponse.builder()
                        .id(1L)
                        .openMatId(10L)
                        .authorUserId(3L)
                        .authorNickname("host")
                        .content("환영합니다")
                        .deleted(false)
                        .editableByMe(false)
                        .deletableByMe(false)
                        .createdAt(LocalDateTime.of(2026, 6, 11, 18, 10))
                        .updatedAt(LocalDateTime.of(2026, 6, 11, 18, 10))
                        .replies(List.of())
                        .build()
        ));

        mockMvc.perform(get("/api/v1/open-mats/10/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].editableByMe").value(false));
    }

    @Test
    @DisplayName("오픈매트 댓글 작성은 인증이 필요하다")
    void createComment_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/open-mats/10/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "댓글 남깁니다"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("오픈매트 댓글 신고는 인증이 필요하다")
    void reportComment_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/open-mats/comments/10/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "SPAM"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private void authenticateUser() {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);
    }

    @Configuration
    @EnableWebMvc
    @Import({OpenMatController.class, GlobalExceptionHandler.class, SecurityConfig.class})
    static class TestConfig {

        @Bean
        OpenMatService openMatService() {
            return mock(OpenMatService.class);
        }

        @Bean
        OpenMatImageUploadService openMatImageUploadService() {
            return mock(OpenMatImageUploadService.class);
        }

        @Bean
        OpenMatCommentService openMatCommentService() {
            return mock(OpenMatCommentService.class);
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
