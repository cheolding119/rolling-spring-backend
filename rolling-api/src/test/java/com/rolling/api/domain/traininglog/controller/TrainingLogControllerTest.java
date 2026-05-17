package com.rolling.api.domain.traininglog.controller;

import com.rolling.api.domain.traininglog.dto.TrainingLogChecklistItem;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryResponse;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.service.TrainingLogService;
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
import org.springframework.data.web.config.SpringDataWebConfiguration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrainingLogControllerTest {

    private MockMvc mockMvc;
    private TrainingLogService trainingLogService;
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

        trainingLogService = context.getBean(TrainingLogService.class);
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
    @DisplayName("훈련 기록 생성은 인증 사용자면 성공한다")
    void create_withUserToken_returnsOk() throws Exception {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);
        given(trainingLogService.create(any(), any(), any())).willReturn(response(1L));

        mockMvc.perform(post("/api/v1/training-logs/me/entries/2026-05-17")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "TECHNIQUE",
                                  "title": "암 트라이앵글 디테일",
                                  "content": "무릎 각도와 팔 위치를 정리했다.",
                                  "checklist": [
                                    {
                                      "text": "디테일 복습",
                                      "checked": true
                                    }
                                  ],
                                  "hashtags": ["triangle"],
                                  "trainingMinutes": 90
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.category").value("TECHNIQUE"))
                .andExpect(jsonPath("$.data.checklist[0].text").value("디테일 복습"));
    }

    @Test
    @DisplayName("훈련 기록 목록 조회 API는 accessToken이 없으면 401을 반환한다")
    void findEntries_withoutAccessToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/training-logs/me/entries/2026-05-17"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("해시태그 자동완성은 인증 사용자면 성공한다")
    void autocompleteTags_withUserToken_returnsOk() throws Exception {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);
        given(trainingLogService.autocompleteTags(2L, "tri")).willReturn(List.of("triangle", "arm-triangle"));

        mockMvc.perform(get("/api/v1/training-logs/me/tags")
                        .header("Authorization", "Bearer user-token")
                        .param("q", "tri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("triangle"))
                .andExpect(jsonPath("$.data[1]").value("arm-triangle"));
    }

    private TrainingLogEntryResponse response(Long id) {
        return TrainingLogEntryResponse.builder()
                .id(id)
                .trainingDate(LocalDate.of(2026, 5, 17))
                .category(TrainingLogCategory.TECHNIQUE)
                .title("암 트라이앵글 디테일")
                .content("무릎 각도와 팔 위치를 정리했다.")
                .checklist(List.of(new TrainingLogChecklistItem("디테일 복습", true)))
                .hashtags(List.of("triangle"))
                .trainingMinutes(90)
                .createdAt(LocalDateTime.of(2026, 5, 17, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 17, 12, 0))
                .build();
    }

    @Configuration
    @EnableWebMvc
    @Import({TrainingLogController.class, GlobalExceptionHandler.class, SecurityConfig.class, SpringDataWebConfiguration.class})
    static class TestConfig {

        @Bean
        TrainingLogService trainingLogService() {
            return mock(TrainingLogService.class);
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
