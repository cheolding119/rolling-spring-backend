package com.rolling.api.domain.traininglog.controller;

import com.rolling.api.domain.traininglog.dto.TrainingLogCalendarDailySummary;
import com.rolling.api.domain.traininglog.dto.TrainingLogCalendarMonthlySummary;
import com.rolling.api.domain.traininglog.dto.TrainingLogCalendarSummaryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogChecklistItem;
import com.rolling.api.domain.traininglog.dto.TrainingLogExternalLink;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogImageUploadUrlResponse;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogLinkType;
import com.rolling.api.domain.traininglog.service.TrainingLogImageUploadService;
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
    private TrainingLogImageUploadService trainingLogImageUploadService;
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
        trainingLogImageUploadService = context.getBean(TrainingLogImageUploadService.class);
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
    @DisplayName("create accepts authenticated user requests")
    void create_withUserToken_returnsOk() throws Exception {
        authenticateUser();
        given(trainingLogService.create(any(), any(), any())).willReturn(response(1L));

        mockMvc.perform(post("/api/v1/training-logs/me/entries/2026-05-17")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "category": "TECHNIQUE",
                                  "title": "Arm Triangle Details",
                                  "content": "Finished details for knee angle and arm position.",
                                  "checklist": [
                                    {
                                      "text": "Triangle Review",
                                      "checked": true
                                    }
                                  ],
                                  "hashtags": ["triangle"],
                                  "externalLinks": [
                                    {
                                      "type": "INSTAGRAM",
                                      "url": "www.instagram.com/p/triangle"
                                    }
                                  ],
                                  "imageUrl": "https://cdn.test.com/training-log.jpg",
                                  "trainingMinutes": 90
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.category").value("TECHNIQUE"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://cdn.test.com/training-log.jpg"))
                .andExpect(jsonPath("$.data.externalLinks[0].type").value("INSTAGRAM"))
                .andExpect(jsonPath("$.data.externalLinks[0].url").value("https://www.instagram.com/p/triangle"))
                .andExpect(jsonPath("$.data.checklist[0].text").value("Triangle Review"));
    }

    @Test
    @DisplayName("find entries requires authentication")
    void findEntries_withoutAccessToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/training-logs/me/entries/2026-05-17"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("autocomplete tags returns normalized suggestions for authenticated users")
    void autocompleteTags_withUserToken_returnsOk() throws Exception {
        authenticateUser();
        given(trainingLogService.autocompleteTags(2L, "tri")).willReturn(List.of("triangle", "arm-triangle"));

        mockMvc.perform(get("/api/v1/training-logs/me/tags")
                        .header("Authorization", "Bearer user-token")
                        .param("q", "tri"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0]").value("triangle"))
                .andExpect(jsonPath("$.data[1]").value("arm-triangle"));
    }

    @Test
    @DisplayName("calendar summary returns aggregated data for authenticated users")
    void calendar_withUserToken_returnsOk() throws Exception {
        authenticateUser();
        given(trainingLogService.getCalendarSummary(2L, 2026)).willReturn(
                TrainingLogCalendarSummaryResponse.builder()
                        .year(2026)
                        .totalTrainingMinutes(180)
                        .activeDays(3)
                        .monthlySummaries(List.of(new TrainingLogCalendarMonthlySummary(5, 150, 2)))
                        .dailySummaries(List.of(new TrainingLogCalendarDailySummary(LocalDate.of(2026, 5, 1), 150, 2)))
                        .build()
        );

        mockMvc.perform(get("/api/v1/training-logs/me/calendar")
                        .header("Authorization", "Bearer user-token")
                        .param("year", "2026"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.totalTrainingMinutes").value(180))
                .andExpect(jsonPath("$.data.activeDays").value(3))
                .andExpect(jsonPath("$.data.monthlySummaries[0].month").value(5));
    }

    @Test
    @DisplayName("recent endpoint returns ordered entries for authenticated users")
    void recent_withUserToken_returnsOk() throws Exception {
        authenticateUser();
        given(trainingLogService.findRecentEntries(2L)).willReturn(List.of(response(2L), response(1L)));

        mockMvc.perform(get("/api/v1/training-logs/me/recent")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[1].id").value(1));
    }

    @Test
    @DisplayName("upload URL endpoint returns presigned URL metadata for authenticated users")
    void createUploadUrl_withUserToken_returnsOk() throws Exception {
        authenticateUser();
        given(trainingLogImageUploadService.createUploadUrl(any())).willReturn(
                TrainingLogImageUploadUrlResponse.builder()
                        .uploadUrl("https://upload.test/presigned")
                        .imageKey("training/logs/images/sample.jpg")
                        .imageUrl("https://cdn.test.com/training/logs/images/sample.jpg")
                        .expiresAt(LocalDateTime.of(2026, 5, 17, 12, 15))
                        .build()
        );

        mockMvc.perform(post("/api/v1/training-logs/me/upload-url")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fileName": "training-log.jpg",
                                  "contentType": "image/jpeg"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.uploadUrl").value("https://upload.test/presigned"))
                .andExpect(jsonPath("$.data.imageKey").value("training/logs/images/sample.jpg"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://cdn.test.com/training/logs/images/sample.jpg"));
    }

    private void authenticateUser() {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);
    }

    private TrainingLogEntryResponse response(Long id) {
        return TrainingLogEntryResponse.builder()
                .id(id)
                .trainingDate(LocalDate.of(2026, 5, 17))
                .category(TrainingLogCategory.TECHNIQUE)
                .title("Arm Triangle Details")
                .content("Finished details for knee angle and arm position.")
                .checklist(List.of(new TrainingLogChecklistItem("Triangle Review", true)))
                .hashtags(List.of("triangle"))
                .externalLinks(List.of(new TrainingLogExternalLink(TrainingLogLinkType.INSTAGRAM, "https://www.instagram.com/p/triangle")))
                .imageUrl("https://cdn.test.com/training-log.jpg")
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
        TrainingLogImageUploadService trainingLogImageUploadService() {
            return mock(TrainingLogImageUploadService.class);
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
