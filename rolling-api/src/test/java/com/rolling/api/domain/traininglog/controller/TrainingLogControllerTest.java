package com.rolling.api.domain.traininglog.controller;

import com.rolling.api.domain.traininglog.dto.TrainingLogChecklistItem;
import com.rolling.api.domain.traininglog.dto.TrainingLogExternalLink;
import com.rolling.api.domain.traininglog.dto.TrainingLogAttendanceGrassDay;
import com.rolling.api.domain.traininglog.dto.TrainingLogAttendanceGrassResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogCategoryInsight;
import com.rolling.api.domain.traininglog.dto.TrainingLogDailyInsight;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntrySummaryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogHashtagInsight;
import com.rolling.api.domain.traininglog.dto.TrainingLogImageUploadUrlResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogInsightPeriod;
import com.rolling.api.domain.traininglog.dto.TrainingLogInsightResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogInsightSummary;
import com.rolling.api.domain.traininglog.dto.TrainingLogMonthlyCalendarDailySummary;
import com.rolling.api.domain.traininglog.dto.TrainingLogMonthlyCalendarResponse;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogColor;
import com.rolling.api.domain.traininglog.entity.TrainingLogLinkType;
import com.rolling.api.domain.traininglog.entity.TrainingLogVisibility;
import com.rolling.api.domain.traininglog.service.TrainingLogImageUploadService;
import com.rolling.api.domain.traininglog.service.TrainingLogInsightService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrainingLogControllerTest {

    private MockMvc mockMvc;
    private TrainingLogService trainingLogService;
    private TrainingLogInsightService trainingLogInsightService;
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
        trainingLogInsightService = context.getBean(TrainingLogInsightService.class);
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
                                  "trainingIntensity": 4,
                                  "gymAttendance": true,
                                  "condition": 3,
                                  "trainingMinutes": 90,
                                  "checklist": [
                                    {
                                      "text": "Triangle Review",
                                      "checked": true,
                                      "favorite": true,
                                      "emoji": "🔥"
                                    }
                                  ],
                                  "hashtags": ["triangle"],
                                  "imageUrls": [
                                    "https://cdn.test.com/training-log-1.jpg",
                                    "https://cdn.test.com/training-log-2.jpg"
                                  ],
                                  "color": "BLUE",
                                  "externalLinks": [
                                    {
                                      "type": "INSTAGRAM",
                                      "url": "www.instagram.com/p/triangle"
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.category").value("TECHNIQUE"))
                .andExpect(jsonPath("$.data.color").value("BLUE"))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.trainingIntensity").value(4))
                .andExpect(jsonPath("$.data.gymAttendance").value(true))
                .andExpect(jsonPath("$.data.condition").value(3))
                .andExpect(jsonPath("$.data.trainingMinutes").value(90))
                .andExpect(jsonPath("$.data.imageUrl").value("https://cdn.test.com/training-log-1.jpg"))
                .andExpect(jsonPath("$.data.imageUrls[0]").value("https://cdn.test.com/training-log-1.jpg"))
                .andExpect(jsonPath("$.data.imageUrls[1]").value("https://cdn.test.com/training-log-2.jpg"))
                .andExpect(jsonPath("$.data.externalLinks[0].type").value("INSTAGRAM"))
                .andExpect(jsonPath("$.data.externalLinks[0].url").value("https://www.instagram.com/p/triangle"))
                .andExpect(jsonPath("$.data.checklist[0].text").value("Triangle Review"))
                .andExpect(jsonPath("$.data.checklist[0].favorite").value(true))
                .andExpect(jsonPath("$.data.checklist[0].emoji").value("🔥"));
    }

    @Test
    @DisplayName("find entries requires authentication")
    void findEntries_withoutAccessToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/training-logs/me/entries")
                        .param("date", "2026-05-17"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("attendance grass returns 365 day data for authenticated users")
    void attendanceGrass_withUserToken_returnsOk() throws Exception {
        authenticateUser();
        given(trainingLogInsightService.getAttendanceGrass(2L, LocalDate.of(2026, 5, 22))).willReturn(attendanceGrassResponse());

        mockMvc.perform(get("/api/v1/training-logs/me/attendance-grass")
                        .header("Authorization", "Bearer user-token")
                        .param("date", "2026-05-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.startDate").value("2025-05-23"))
                .andExpect(jsonPath("$.data.endDate").value("2026-05-22"))
                .andExpect(jsonPath("$.data.totalDays").value(365))
                .andExpect(jsonPath("$.data.attendanceDays").value(1))
                .andExpect(jsonPath("$.data.currentStreakDays").value(1))
                .andExpect(jsonPath("$.data.longestStreakDays").value(1))
                .andExpect(jsonPath("$.data.recent30DaysAttendanceDays").value(1))
                .andExpect(jsonPath("$.data.days[0].date").value("2026-05-22"))
                .andExpect(jsonPath("$.data.days[0].dayOfWeek").value("FRI"))
                .andExpect(jsonPath("$.data.days[0].attended").value(true))
                .andExpect(jsonPath("$.data.days[0].level").value(2))
                .andExpect(jsonPath("$.data.days[0].recordCount").value(1))
                .andExpect(jsonPath("$.data.days[0].totalTrainingMinutes").value(90))
                .andExpect(jsonPath("$.data.days[0].averageTrainingIntensity").value(3.0))
                .andExpect(jsonPath("$.data.days[0].averageCondition").value(4.0))
                .andExpect(jsonPath("$.data.days[0].categories[0]").value("TECHNIQUE"));
    }

    @Test
    @DisplayName("attendance grass requires authentication")
    void attendanceGrass_withoutAccessToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/training-logs/me/attendance-grass")
                        .param("date", "2026-05-22"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("insights returns weekly insight data for authenticated users")
    void insights_withUserToken_returnsOk() throws Exception {
        authenticateUser();
        given(trainingLogInsightService.getInsights(
                2L,
                TrainingLogInsightPeriod.WEEK,
                LocalDate.of(2026, 5, 22)
        )).willReturn(insightResponse());

        mockMvc.perform(get("/api/v1/training-logs/me/insights")
                        .header("Authorization", "Bearer user-token")
                        .param("period", "WEEK")
                        .param("date", "2026-05-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.period").value("WEEK"))
                .andExpect(jsonPath("$.data.startDate").value("2026-05-18"))
                .andExpect(jsonPath("$.data.endDate").value("2026-05-24"))
                .andExpect(jsonPath("$.data.summary.recordCount").value(2))
                .andExpect(jsonPath("$.data.summary.trainingDays").value(1))
                .andExpect(jsonPath("$.data.summary.attendanceDays").value(1))
                .andExpect(jsonPath("$.data.summary.attendanceRate").value(14.3))
                .andExpect(jsonPath("$.data.summary.totalTrainingMinutes").value(90))
                .andExpect(jsonPath("$.data.summary.averageTrainingMinutesPerAttendanceDay").value(90.0))
                .andExpect(jsonPath("$.data.summary.averageTrainingIntensity").value(3.0))
                .andExpect(jsonPath("$.data.summary.averageCondition").value(4.0))
                .andExpect(jsonPath("$.data.summary.checklistCompletionRate").value(50.0))
                .andExpect(jsonPath("$.data.dailyStats[0].date").value("2026-05-18"))
                .andExpect(jsonPath("$.data.dailyStats[0].gymAttendance").value(true))
                .andExpect(jsonPath("$.data.dailyStats[0].categories[0]").value("TECHNIQUE"))
                .andExpect(jsonPath("$.data.categoryBreakdown[0].category").value("TECHNIQUE"))
                .andExpect(jsonPath("$.data.categoryBreakdown[0].recordCount").value(2))
                .andExpect(jsonPath("$.data.topHashtags[0].tag").value("guard-pass"))
                .andExpect(jsonPath("$.data.topHashtags[0].count").value(2));
    }

    @Test
    @DisplayName("insights requires authentication")
    void insights_withoutAccessToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/training-logs/me/insights")
                        .param("period", "WEEK")
                        .param("date", "2026-05-22"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("insights rejects unsupported period values")
    void insights_withInvalidPeriod_returnsBadRequest() throws Exception {
        authenticateUser();

        mockMvc.perform(get("/api/v1/training-logs/me/insights")
                        .header("Authorization", "Bearer user-token")
                        .param("period", "YEAR")
                        .param("date", "2026-05-22"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("insights rejects invalid date values")
    void insights_withInvalidDate_returnsBadRequest() throws Exception {
        authenticateUser();

        mockMvc.perform(get("/api/v1/training-logs/me/insights")
                        .header("Authorization", "Bearer user-token")
                        .param("period", "WEEK")
                        .param("date", "2026-13-40"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("find entries returns summary cards for authenticated users")
    void findEntries_withUserToken_returnsOk() throws Exception {
        authenticateUser();
        given(trainingLogService.findEntrySummaries(2L, LocalDate.of(2026, 5, 17))).willReturn(List.of(
                summaryResponse(2L),
                summaryResponse(1L)
        ));

        mockMvc.perform(get("/api/v1/training-logs/me/entries")
                        .header("Authorization", "Bearer user-token")
                        .param("date", "2026-05-17"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].title").value("Arm Triangle Details"))
                .andExpect(jsonPath("$.data[0].category").value("TECHNIQUE"))
                .andExpect(jsonPath("$.data[0].color").value("BLUE"))
                .andExpect(jsonPath("$.data[1].id").value(1));
    }

    @Test
    @DisplayName("find entry detail returns full response for authenticated users")
    void findEntryDetail_withUserToken_returnsOk() throws Exception {
        authenticateUser();
        given(trainingLogService.findEntryDetail(2L, 1L)).willReturn(detailResponse(1L));

        mockMvc.perform(get("/api/v1/training-logs/me/entries/1")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.trainingDate").value("2026-05-17"))
                .andExpect(jsonPath("$.data.visibility").value("PRIVATE"))
                .andExpect(jsonPath("$.data.gymAttendance").value(true))
                .andExpect(jsonPath("$.data.condition").value(3))
                .andExpect(jsonPath("$.data.trainingMinutes").value(90))
                .andExpect(jsonPath("$.data.likeCount").value(3))
                .andExpect(jsonPath("$.data.commentCount").value(5))
                .andExpect(jsonPath("$.data.likedByMe").value(false))
                .andExpect(jsonPath("$.data.commentableByMe").value(false))
                .andExpect(jsonPath("$.data.checklist[0].favorite").value(true))
                .andExpect(jsonPath("$.data.imageUrls[0]").value("https://cdn.test.com/training-log-1.jpg"));
    }

    @Test
    @DisplayName("update accepts training attendance and condition fields")
    void update_withTrainingStatusFields_returnsOk() throws Exception {
        authenticateUser();
        given(trainingLogService.update(any(), any(), any())).willReturn(response(1L));

        mockMvc.perform(patch("/api/v1/training-logs/me/entries/1")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "gymAttendance": true,
                                  "condition": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.gymAttendance").value(true))
                .andExpect(jsonPath("$.data.condition").value(3));
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
    @DisplayName("calendar summary returns month data for authenticated users")
    void calendar_withUserToken_returnsOk() throws Exception {
        authenticateUser();
        given(trainingLogService.getMonthlyCalendarSummary(2L, 2026, 5)).willReturn(
                                TrainingLogMonthlyCalendarResponse.builder()
                                        .year(2026)
                                        .month(5)
                                        .dailySummaries(List.of(
                                                new TrainingLogMonthlyCalendarDailySummary(
                                                        LocalDate.of(2026, 5, 1),
                                                        List.of(TrainingLogColor.BLUE, TrainingLogColor.RED),
                                                        List.of(TrainingLogCategory.TECHNIQUE, TrainingLogCategory.PROMOTION),
                                                        2
                                                )
                                        ))
                                        .build()
        );

        mockMvc.perform(get("/api/v1/training-logs/me/calendar")
                        .header("Authorization", "Bearer user-token")
                        .param("year", "2026")
                        .param("month", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.month").value(5))
                .andExpect(jsonPath("$.data.dailySummaries[0].date").value("2026-05-01"))
                .andExpect(jsonPath("$.data.dailySummaries[0].colors[0]").value("BLUE"))
                .andExpect(jsonPath("$.data.dailySummaries[0].categories[0]").value("TECHNIQUE"))
                .andExpect(jsonPath("$.data.dailySummaries[0].recordCount").value(2));
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
                .color(TrainingLogColor.BLUE)
                .visibility(TrainingLogVisibility.PRIVATE)
                .trainingIntensity(4)
                .gymAttendance(true)
                .condition(3)
                .trainingMinutes(90)
                .title("Arm Triangle Details")
                .content("Finished details for knee angle and arm position.")
                .checklist(List.of(new TrainingLogChecklistItem("Triangle Review", true, true, "🔥")))
                .hashtags(List.of("triangle"))
                .imageUrls(List.of(
                        "https://cdn.test.com/training-log-1.jpg",
                        "https://cdn.test.com/training-log-2.jpg"
                ))
                        .externalLinks(List.of(new TrainingLogExternalLink(TrainingLogLinkType.INSTAGRAM, "https://www.instagram.com/p/triangle")))
                        .imageUrl("https://cdn.test.com/training-log-1.jpg")
                        .build();
    }

    private TrainingLogEntryResponse detailResponse(Long id) {
        return TrainingLogEntryResponse.builder()
                .id(id)
                .trainingDate(LocalDate.of(2026, 5, 17))
                .category(TrainingLogCategory.TECHNIQUE)
                .color(TrainingLogColor.BLUE)
                .visibility(TrainingLogVisibility.PRIVATE)
                .trainingIntensity(4)
                .gymAttendance(true)
                .condition(3)
                .trainingMinutes(90)
                .title("Arm Triangle Details")
                .content("Finished details for knee angle and arm position.")
                .checklist(List.of(new TrainingLogChecklistItem("Triangle Review", true, true, "🔥")))
                .hashtags(List.of("triangle"))
                .imageUrls(List.of(
                        "https://cdn.test.com/training-log-1.jpg",
                        "https://cdn.test.com/training-log-2.jpg"
                ))
                .externalLinks(List.of(new TrainingLogExternalLink(TrainingLogLinkType.INSTAGRAM, "https://www.instagram.com/p/triangle")))
                .imageUrl("https://cdn.test.com/training-log-1.jpg")
                .likeCount(3L)
                .commentCount(5L)
                .likedByMe(false)
                .commentableByMe(false)
                .build();
    }

    private TrainingLogEntrySummaryResponse summaryResponse(Long id) {
        return TrainingLogEntrySummaryResponse.builder()
                .id(id)
                .title("Arm Triangle Details")
                .content("Finished details for knee angle and arm position.")
                .category(TrainingLogCategory.TECHNIQUE)
                .color(TrainingLogColor.BLUE)
                .createdAt(LocalDateTime.of(2026, 5, 17, 12, 0))
                .build();
    }

    private TrainingLogAttendanceGrassResponse attendanceGrassResponse() {
        return TrainingLogAttendanceGrassResponse.builder()
                .startDate(LocalDate.of(2025, 5, 23))
                .endDate(LocalDate.of(2026, 5, 22))
                .totalDays(365)
                .attendanceDays(1)
                .currentStreakDays(1)
                .longestStreakDays(1)
                .recent30DaysAttendanceDays(1)
                .days(List.of(new TrainingLogAttendanceGrassDay(
                        LocalDate.of(2026, 5, 22),
                        "FRI",
                        true,
                        2,
                        1,
                        90,
                        3.0,
                        4.0,
                        List.of(TrainingLogCategory.TECHNIQUE)
                )))
                .build();
    }

    private TrainingLogInsightResponse insightResponse() {
        return TrainingLogInsightResponse.builder()
                .period(TrainingLogInsightPeriod.WEEK)
                .startDate(LocalDate.of(2026, 5, 18))
                .endDate(LocalDate.of(2026, 5, 24))
                .summary(TrainingLogInsightSummary.builder()
                        .recordCount(2)
                        .trainingDays(1)
                        .attendanceDays(1)
                        .attendanceRate(14.3)
                        .totalTrainingMinutes(90)
                        .averageTrainingMinutesPerAttendanceDay(90.0)
                        .averageTrainingIntensity(3.0)
                        .averageCondition(4.0)
                        .checklistCompletionRate(50.0)
                        .build())
                .dailyStats(List.of(new TrainingLogDailyInsight(
                        LocalDate.of(2026, 5, 18),
                        2,
                        true,
                        90,
                        3.0,
                        4.0,
                        List.of(TrainingLogCategory.TECHNIQUE)
                )))
                .categoryBreakdown(List.of(new TrainingLogCategoryInsight(
                        TrainingLogCategory.TECHNIQUE,
                        2,
                        90
                )))
                .topHashtags(List.of(new TrainingLogHashtagInsight("guard-pass", 2)))
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
        TrainingLogInsightService trainingLogInsightService() {
            return mock(TrainingLogInsightService.class);
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
