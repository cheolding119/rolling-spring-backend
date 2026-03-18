package com.rolling.api.domain.notice.controller;

import com.rolling.api.domain.notice.dto.NoticeResponse;
import com.rolling.api.domain.notice.service.NoticeService;
import com.rolling.api.domain.openmat.config.OpenMatTestingAccessConfig;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NoticeAdminControllerTest {

    private MockMvc mockMvc;
    private NoticeService noticeService;
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

        noticeService = context.getBean(NoticeService.class);
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
    @DisplayName("공지사항 생성은 admin key가 있으면 성공한다")
    void create_withAdminKey_returnsOk() throws Exception {
        NoticeResponse response = response(1L, "3월 점검 안내", "3월 20일 02:00부터 점검이 진행됩니다.", "Rolling Admin");
        given(adminAccessConfig.matchesAdminApiKey("notice-secret")).willReturn(true);
        given(noticeService.create(any())).willReturn(response);

        mockMvc.perform(post("/api/v1/notices")
                        .header("X-Crawler-Admin-Key", "notice-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "3월 점검 안내",
                                  "content": "3월 20일 02:00부터 점검이 진행됩니다.",
                                  "authorName": "Rolling Admin"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("3월 점검 안내"));
    }

    @Test
    @DisplayName("공지사항 수정은 admin key가 있으면 성공한다")
    void update_withAdminKey_returnsOk() throws Exception {
        NoticeResponse response = response(2L, "수정된 제목", "수정된 본문", "Rolling Team");
        given(adminAccessConfig.matchesAdminApiKey("notice-secret")).willReturn(true);
        given(noticeService.update(any(), any())).willReturn(response);

        mockMvc.perform(put("/api/v1/notices/2")
                        .header("X-Crawler-Admin-Key", "notice-secret")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "수정된 제목",
                                  "authorName": "Rolling Team"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(2))
                .andExpect(jsonPath("$.data.authorName").value("Rolling Team"));
    }

    @Test
    @DisplayName("공지사항 삭제는 admin key가 있으면 성공한다")
    void delete_withAdminKey_returnsOk() throws Exception {
        given(adminAccessConfig.matchesAdminApiKey("notice-secret")).willReturn(true);

        mockMvc.perform(delete("/api/v1/notices/3")
                        .header("X-Crawler-Admin-Key", "notice-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("공지사항 운영 API는 admin key가 없으면 401을 반환한다")
    void create_withoutAdminKey_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "3월 점검 안내",
                                  "content": "3월 20일 02:00부터 점검이 진행됩니다.",
                                  "authorName": "Rolling Admin"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("공지사항 운영 API는 admin accessToken만으로는 통과할 수 없다")
    void create_withBearerAdminTokenOnly_returnsForbidden() throws Exception {
        given(jwtTokenProvider.validateToken("admin-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("admin-token")).willReturn(1L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(1L)).willReturn(true);
        given(adminAccessConfig.isAdmin(1L)).willReturn(true);

        mockMvc.perform(post("/api/v1/notices")
                        .header("Authorization", "Bearer admin-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "3월 점검 안내",
                                  "content": "3월 20일 02:00부터 점검이 진행됩니다.",
                                  "authorName": "Rolling Admin"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    private NoticeResponse response(Long id, String title, String content, String authorName) {
        return NoticeResponse.builder()
                .id(id)
                .title(title)
                .content(content)
                .authorName(authorName)
                .createdAt(LocalDateTime.of(2026, 3, 18, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 18, 12, 0))
                .build();
    }

    @Configuration
    @EnableWebMvc
    @Import({NoticeAdminController.class, GlobalExceptionHandler.class, SecurityConfig.class})
    static class TestConfig {

        @Bean
        NoticeService noticeService() {
            return mock(NoticeService.class);
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

        @Bean
        OpenMatTestingAccessConfig openMatTestingAccessConfig() {
            return mock(OpenMatTestingAccessConfig.class);
        }
    }
}
