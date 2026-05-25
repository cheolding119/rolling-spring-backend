package com.rolling.api.domain.traininglog.controller;

import com.rolling.api.domain.traininglog.dto.FriendSearchResultResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogCommentResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntrySummaryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogFriendEntrySummaryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogFriendSharingResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogMonthlyCalendarDailySummary;
import com.rolling.api.domain.traininglog.dto.TrainingLogMonthlyCalendarResponse;
import com.rolling.api.domain.traininglog.entity.FriendSearchRelationshipStatus;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogColor;
import com.rolling.api.domain.traininglog.service.TrainingLogSocialService;
import com.rolling.api.domain.user.entity.BeltColor;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrainingLogSocialControllerTest {

    private MockMvc mockMvc;
    private TrainingLogSocialService trainingLogSocialService;
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

        trainingLogSocialService = context.getBean(TrainingLogSocialService.class);
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
    @DisplayName("친구 검색은 인증이 필요하다")
    void searchFriends_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/friends/search").param("q", "민준"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("친구 검색은 이름, 소속, 벨트를 응답한다")
    void searchFriends_withToken_returnsResults() throws Exception {
        authenticateUser();
        given(trainingLogSocialService.searchFriends(2L, "민준")).willReturn(List.of(
                FriendSearchResultResponse.builder()
                        .userId(10L)
                        .nickname("민준")
                        .affiliation("롤링 주짓수")
                        .beltColor(BeltColor.BLUE)
                        .friendRequestStatus(FriendSearchRelationshipStatus.PENDING_SENT)
                        .outgoingRequestId(88L)
                        .build()
        ));

        mockMvc.perform(get("/api/v1/friends/search")
                        .header("Authorization", "Bearer user-token")
                        .param("q", "민준"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].userId").value(10L))
                .andExpect(jsonPath("$.data[0].nickname").value("민준"))
                .andExpect(jsonPath("$.data[0].affiliation").value("롤링 주짓수"))
                .andExpect(jsonPath("$.data[0].beltColor").value("BLUE"))
                .andExpect(jsonPath("$.data[0].friendRequestStatus").value("PENDING_SENT"))
                .andExpect(jsonPath("$.data[0].outgoingRequestId").value(88L));
    }

    @Test
    @DisplayName("친구 요청 취소는 인증 사용자에게 허용된다")
    void cancelFriendRequest_withToken_returnsSuccess() throws Exception {
        authenticateUser();

        mockMvc.perform(post("/api/v1/friends/requests/88/cancel")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("친구 공개 설정 조회는 설정 응답을 반환한다")
    void getFriendSharing_withToken_returnsSetting() throws Exception {
        authenticateUser();
        given(trainingLogSocialService.getFriendSharing(2L)).willReturn(
                TrainingLogFriendSharingResponse.builder()
                        .shareWithFriends(true)
                        .updatedAt(LocalDateTime.of(2026, 5, 24, 12, 0))
                        .build()
        );

        mockMvc.perform(get("/api/v1/training-logs/me/friend-sharing")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shareWithFriends").value(true))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-05-24T12:00:00"));
    }

    @Test
    @DisplayName("친구 공개 설정 변경은 인증 사용자에게 허용된다")
    void updateFriendSharing_withToken_returnsUpdatedSetting() throws Exception {
        authenticateUser();
        given(trainingLogSocialService.updateFriendSharing(eq(2L), any())).willReturn(
                TrainingLogFriendSharingResponse.builder()
                        .shareWithFriends(false)
                        .updatedAt(LocalDateTime.of(2026, 5, 24, 13, 0))
                        .build()
        );

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch("/api/v1/training-logs/me/friend-sharing")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shareWithFriends": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.shareWithFriends").value(false))
                .andExpect(jsonPath("$.data.updatedAt").value("2026-05-24T13:00:00"));
    }

    @Test
    @DisplayName("친구 피드는 페이징 응답을 반환한다")
    void friendFeed_withToken_returnsPagedEntries() throws Exception {
        authenticateUser();
        given(trainingLogSocialService.findFriendFeed(eq(2L), any())).willReturn(new PageImpl<>(
                List.of(
                        TrainingLogFriendEntrySummaryResponse.builder()
                                .id(1L)
                                .authorUserId(10L)
                                .authorNickname("민준")
                                .trainingDate(LocalDate.of(2026, 5, 22))
                                .category(TrainingLogCategory.TECHNIQUE)
                                .title("오늘 연습")
                                .content("암바 디테일")
                                .likeCount(2L)
                                .commentCount(1L)
                                .likedByMe(true)
                                .createdAt(LocalDateTime.of(2026, 5, 22, 12, 0))
                                .build()
                ),
                org.springframework.data.domain.PageRequest.of(0, 20),
                1
        ));

        mockMvc.perform(get("/api/v1/training-logs/friends")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].authorNickname").value("민준"))
                .andExpect(jsonPath("$.data.content[0].likedByMe").value(true));
    }

    @Test
    @DisplayName("친구 월간 캘린더 조회는 기존 캘린더 응답 구조를 반환한다")
    void friendCalendar_withToken_returnsMonthlyCalendar() throws Exception {
        authenticateUser();
        given(trainingLogSocialService.getFriendMonthlyCalendar(2L, 10L, 2026, 5)).willReturn(
                TrainingLogMonthlyCalendarResponse.builder()
                        .year(2026)
                        .month(5)
                        .dailySummaries(List.of(
                                new TrainingLogMonthlyCalendarDailySummary(
                                        LocalDate.of(2026, 5, 22),
                                        List.of(TrainingLogColor.BLUE),
                                        List.of(TrainingLogCategory.TECHNIQUE),
                                        2
                                )
                        ))
                        .build()
        );

        mockMvc.perform(get("/api/v1/training-logs/friends/10/calendar")
                        .header("Authorization", "Bearer user-token")
                        .param("year", "2026")
                        .param("month", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.month").value(5))
                .andExpect(jsonPath("$.data.dailySummaries[0].date").value("2026-05-22"))
                .andExpect(jsonPath("$.data.dailySummaries[0].recordCount").value(2));
    }

    @Test
    @DisplayName("친구 날짜별 기록 조회는 기존 요약 응답 구조를 반환한다")
    void friendEntriesByDate_withToken_returnsSummaries() throws Exception {
        authenticateUser();
        given(trainingLogSocialService.findFriendEntriesByDate(2L, 10L, LocalDate.of(2026, 5, 22))).willReturn(List.of(
                TrainingLogEntrySummaryResponse.builder()
                        .id(1L)
                        .title("오늘 연습")
                        .content("암바 디테일")
                        .category(TrainingLogCategory.TECHNIQUE)
                        .color(TrainingLogColor.BLUE)
                        .createdAt(LocalDateTime.of(2026, 5, 22, 12, 0))
                        .build()
        ));

        mockMvc.perform(get("/api/v1/training-logs/friends/10/entries")
                        .header("Authorization", "Bearer user-token")
                        .param("date", "2026-05-22"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1L))
                .andExpect(jsonPath("$.data[0].title").value("오늘 연습"))
                .andExpect(jsonPath("$.data[0].color").value("BLUE"));
    }

    @Test
    @DisplayName("댓글 작성은 인증 사용자에게 허용된다")
    void createComment_withToken_returnsComment() throws Exception {
        authenticateUser();
        given(trainingLogSocialService.createComment(any(), any(), any())).willReturn(
                TrainingLogCommentResponse.builder()
                        .id(100L)
                        .entryId(1L)
                        .authorUserId(2L)
                        .authorNickname("viewer")
                        .content("좋은 기록이네요")
                        .deleted(false)
                        .editableByMe(true)
                        .deletableByMe(true)
                        .createdAt(LocalDateTime.of(2026, 5, 23, 12, 0))
                        .updatedAt(LocalDateTime.of(2026, 5, 23, 12, 0))
                        .replies(List.of())
                        .build()
        );

        mockMvc.perform(post("/api/v1/training-logs/entries/1/comments")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "content": "좋은 기록이네요"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(100L))
                .andExpect(jsonPath("$.data.content").value("좋은 기록이네요"))
                .andExpect(jsonPath("$.data.editableByMe").value(true));
    }

    @Test
    @DisplayName("댓글 신고는 인증이 필요하다")
    void reportComment_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/training-logs/comments/100/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "SPAM"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("댓글 신고는 인증 사용자에게 허용된다")
    void reportComment_withToken_returnsSuccess() throws Exception {
        authenticateUser();

        mockMvc.perform(post("/api/v1/training-logs/comments/100/report")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "SPAM"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    private void authenticateUser() {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);
    }

    @Configuration
    @EnableWebMvc
    @Import({TrainingLogSocialController.class, GlobalExceptionHandler.class, SecurityConfig.class, SpringDataWebConfiguration.class})
    static class TestConfig {

        @Bean
        TrainingLogSocialService trainingLogSocialService() {
            return mock(TrainingLogSocialService.class);
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
