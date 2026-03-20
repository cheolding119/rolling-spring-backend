package com.rolling.api.global.security;

import com.rolling.api.domain.notice.controller.NoticeAdminController;
import com.rolling.api.domain.notice.controller.NoticeController;
import com.rolling.api.domain.notice.dto.NoticeResponse;
import com.rolling.api.domain.notice.service.NoticeService;
import com.rolling.api.domain.openmat.controller.OpenMatController;
import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.openmat.service.OpenMatService;
import com.rolling.api.domain.tournament.controller.TournamentCrawlerController;
import com.rolling.api.domain.tournament.dto.TournamentCrawlResult;
import com.rolling.api.domain.tournament.service.TournamentManagerService;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.config.SecurityConfig;
import com.rolling.api.global.exception.GlobalExceptionHandler;
import com.rolling.api.global.logging.RequestTrackingFilter;
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

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityAuthorizationIntegrationTest {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;
    private OpenMatService openMatService;
    private NoticeService noticeService;
    private TournamentManagerService tournamentManagerService;
    private UserRepository userRepository;
    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigWebApplicationContext();
        context.setServletContext(new MockServletContext());
        TestPropertyValues.of(
                "jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
                "jwt.access-token-expiry=1800000",
                "jwt.refresh-token-expiry=1209600000",
                "admin.user-ids=1"
        ).applyTo(context);
        context.register(TestConfig.class);
        context.refresh();

        openMatService = context.getBean(OpenMatService.class);
        noticeService = context.getBean(NoticeService.class);
        tournamentManagerService = context.getBean(TournamentManagerService.class);
        userRepository = context.getBean(UserRepository.class);
        jwtTokenProvider = context.getBean(JwtTokenProvider.class);

        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        given(userRepository.existsByIdAndIsWithdrawnFalse(anyLong())).willReturn(true);
    }

    @AfterEach
    void tearDown() {
        if (context != null) {
            context.close();
        }
    }

    @Test
    @DisplayName("공개 엔드포인트는 accessToken 없이 접근할 수 있다")
    void publicEndpoints_areAccessibleWithoutToken() throws Exception {
        stubNoticeResponse();
        given(openMatService.findById(eq(1L), isNull(), eq(false))).willReturn(
                OpenMatResponse.builder()
                        .id(1L)
                        .title("주말 오픈매트")
                        .description("오픈")
                        .startDateTime(LocalDateTime.of(2026, 3, 22, 14, 0))
                        .endDateTime(LocalDateTime.of(2026, 3, 22, 16, 0))
                        .locationName("Rolling Gym")
                        .address("Seoul")
                        .region(Region.SEOUL)
                        .maxCapacity(10)
                        .currentParticipants(2)
                        .status(OpenMatStatus.RECRUITING)
                        .reported(false)
                        .hostId(1L)
                        .hostNickname("host")
                        .deleted(false)
                        .createdAt(LocalDateTime.of(2026, 3, 20, 9, 0))
                        .build()
        );

        mockMvc.perform(get("/api/v1/notices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        mockMvc.perform(get("/api/v1/open-mats/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("requestId 헤더가 없으면 서버가 새 값을 발급한다")
    void requestId_isGeneratedWhenMissing() throws Exception {
        stubNoticeResponse();

        mockMvc.perform(get("/api/v1/notices/1"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestTrackingFilter.REQUEST_ID_HEADER, not(isEmptyOrNullString())))
                .andExpect(header().string(RequestTrackingFilter.TRACE_ID_HEADER, not(isEmptyOrNullString())));
    }

    @Test
    @DisplayName("외부 requestId 헤더가 있으면 같은 값을 응답 헤더로 유지한다")
    void requestId_reusesIncomingHeader() throws Exception {
        stubNoticeResponse();

        mockMvc.perform(get("/api/v1/notices/1")
                        .header(RequestTrackingFilter.REQUEST_ID_HEADER, "client-trace-001"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestTrackingFilter.REQUEST_ID_HEADER, "client-trace-001"))
                .andExpect(header().string(RequestTrackingFilter.TRACE_ID_HEADER, "client-trace-001"));
    }

    @Test
    @DisplayName("내 오픈매트 목록은 공개 상세 경로보다 우선해 인증을 요구한다")
    void myOpenMats_requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/open-mats/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("인증 사용자는 오픈매트 신청 API에 접근할 수 있다")
    void apply_withUserToken_returnsOk() throws Exception {
        mockMvc.perform(post("/api/v1/open-mats/11/apply")
                        .header("Authorization", bearerToken(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @DisplayName("관리자 전용 엔드포인트는 일반 사용자 accessToken이면 403을 반환한다")
    void adminEndpoints_withUserToken_returnForbidden() throws Exception {
        mockMvc.perform(post("/api/v1/tournaments/crawl")
                        .header("Authorization", bearerToken(2L)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/notices")
                        .header("Authorization", bearerToken(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"title\": \"운영 공지\",
                                  \"content\": \"본문\",
                                  \"authorName\": \"Rolling Admin\"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("관리자 전용 엔드포인트는 admin accessToken이면 성공한다")
    void adminEndpoints_withAdminToken_returnOk() throws Exception {
        given(tournamentManagerService.crawlAndSaveAll()).willReturn(TournamentCrawlResult.builder()
                .crawledCount(5)
                .createdCount(2)
                .updatedCount(2)
                .skippedCount(1)
                .build());
        given(noticeService.create(any())).willReturn(NoticeResponse.builder()
                .id(9L)
                .title("운영 공지")
                .content("본문")
                .authorName("Rolling Admin")
                .createdAt(LocalDateTime.of(2026, 3, 20, 12, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 20, 12, 0))
                .build());

        mockMvc.perform(post("/api/v1/tournaments/crawl")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.crawledCount").value(5));

        mockMvc.perform(post("/api/v1/notices")
                        .header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"title\": \"운영 공지\",
                                  \"content\": \"본문\",
                                  \"authorName\": \"Rolling Admin\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(9));
    }

    private void stubNoticeResponse() {
        given(noticeService.findById(1L)).willReturn(NoticeResponse.builder()
                .id(1L)
                .title("공지")
                .content("본문")
                .authorName("Rolling Admin")
                .createdAt(LocalDateTime.of(2026, 3, 20, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 20, 10, 0))
                .build());
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

    @Configuration
    @EnableWebMvc
    @Import({
            SecurityConfig.class,
            GlobalExceptionHandler.class,
            JwtTokenProvider.class,
            AdminAccessConfig.class,
            OpenMatController.class,
            NoticeController.class,
            NoticeAdminController.class,
            TournamentCrawlerController.class
    })
    static class TestConfig {

        @Bean
        OpenMatService openMatService() {
            return mock(OpenMatService.class);
        }

        @Bean
        NoticeService noticeService() {
            return mock(NoticeService.class);
        }

        @Bean
        TournamentManagerService tournamentManagerService() {
            return mock(TournamentManagerService.class);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }
    }
}
