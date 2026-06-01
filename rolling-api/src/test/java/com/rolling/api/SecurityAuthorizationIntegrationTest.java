package com.rolling.api.global.security;

import com.rolling.api.domain.notice.controller.NoticeAdminController;
import com.rolling.api.domain.notice.controller.NoticeController;
import com.rolling.api.domain.notice.dto.NoticeListItemResponse;
import com.rolling.api.domain.notice.dto.NoticeResponse;
import com.rolling.api.domain.notice.service.NoticeService;
import com.rolling.api.domain.community.controller.CommunityController;
import com.rolling.api.domain.community.controller.CommunityAdminController;
import com.rolling.api.domain.community.dto.CommunityAdminCommentResponse;
import com.rolling.api.domain.community.dto.CommunityAdminPostResponse;
import com.rolling.api.domain.community.dto.CommunityCommentReportAdminResponse;
import com.rolling.api.domain.community.dto.CommunityCommentResponse;
import com.rolling.api.domain.community.dto.CommunityPostDetailResponse;
import com.rolling.api.domain.community.dto.CommunityPostReportAdminResponse;
import com.rolling.api.domain.community.dto.CommunityPostSummaryResponse;
import com.rolling.api.domain.community.entity.CommunityPostCategory;
import com.rolling.api.domain.community.entity.CommunityCommentStatus;
import com.rolling.api.domain.community.entity.CommunityPostStatus;
import com.rolling.api.domain.community.service.CommunityPostImageUploadService;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.community.service.CommunityService;
import com.rolling.api.domain.map.controller.MapController;
import com.rolling.api.domain.map.service.KakaoGeocodeService;
import com.rolling.api.domain.traininglog.controller.TrainingLogAdminController;
import com.rolling.api.domain.traininglog.dto.TrainingLogCommentReportAdminResponse;
import com.rolling.api.domain.traininglog.service.TrainingLogSocialService;
import com.rolling.api.domain.openmat.controller.OpenMatController;
import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.openmat.service.OpenMatService;
import com.rolling.api.domain.seminar.controller.SeminarController;
import com.rolling.api.domain.seminar.dto.SeminarResponse;
import com.rolling.api.domain.seminar.entity.SeminarStatus;
import com.rolling.api.domain.seminar.service.SeminarService;
import com.rolling.api.domain.tournament.controller.TournamentController;
import com.rolling.api.domain.tournament.controller.TournamentCrawlerController;
import com.rolling.api.domain.tournament.dto.TournamentCrawlResult;
import com.rolling.api.domain.tournament.dto.TournamentResponse;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.service.TournamentFavoriteService;
import com.rolling.api.domain.tournament.service.TournamentManagerService;
import com.rolling.api.domain.tournament.service.TournamentService;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SecurityAuthorizationIntegrationTest {

    private AnnotationConfigWebApplicationContext context;
    private MockMvc mockMvc;
    private OpenMatService openMatService;
    private SeminarService seminarService;
    private NoticeService noticeService;
    private CommunityService communityService;
    private TrainingLogSocialService trainingLogSocialService;
    private TournamentService tournamentService;
    private TournamentManagerService tournamentManagerService;
    private KakaoGeocodeService kakaoGeocodeService;
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
        seminarService = context.getBean(SeminarService.class);
        noticeService = context.getBean(NoticeService.class);
        communityService = context.getBean(CommunityService.class);
        trainingLogSocialService = context.getBean(TrainingLogSocialService.class);
        tournamentService = context.getBean(TournamentService.class);
        tournamentManagerService = context.getBean(TournamentManagerService.class);
        kakaoGeocodeService = context.getBean(KakaoGeocodeService.class);
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
    @DisplayName("공개 조회 엔드포인트는 accessToken 없이 접근할 수 있다")
    void publicEndpoints_areAccessibleWithoutToken() throws Exception {
        stubOpenMatResponses();
        stubSeminarResponses();
        stubTournamentResponses();
        stubNoticeResponses();
        stubCommunityResponses();

        mockMvc.perform(get("/api/v1/open-mats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        mockMvc.perform(get("/api/v1/open-mats/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("주말 오픈매트"))
                .andExpect(jsonPath("$.data.startDateTime").value("2026-03-22T14:00:00"))
                .andExpect(jsonPath("$.data.locationName").value("Rolling Gym"))
                .andExpect(jsonPath("$.data.address").value("Seoul"))
                .andExpect(jsonPath("$.data.hostNickname").value("host"))
                .andExpect(jsonPath("$.data.deleted").value(false));

        mockMvc.perform(get("/api/v1/seminars"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        mockMvc.perform(get("/api/v1/seminars/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("가드 패스 세미나"))
                .andExpect(jsonPath("$.data.instructorName").value("김코치"))
                .andExpect(jsonPath("$.data.locationName").value("Rolling Gym"))
                .andExpect(jsonPath("$.data.deleted").value(false));

        mockMvc.perform(get("/api/v1/tournaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        mockMvc.perform(get("/api/v1/tournaments/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("국내 대회"))
                .andExpect(jsonPath("$.data.competitionDate").value("2026-04-05"))
                .andExpect(jsonPath("$.data.registrationDeadline").value("2026-03-31"))
                .andExpect(jsonPath("$.data.location").value("Seoul"))
                .andExpect(jsonPath("$.data.posterUrl").value("https://cdn.example.com/tournaments/1.jpg"));

        mockMvc.perform(get("/api/v1/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        mockMvc.perform(get("/api/v1/notices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1));

        mockMvc.perform(get("/api/v1/community/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        mockMvc.perform(get("/api/v1/community/posts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.authorNickname").value("community-user"));

        mockMvc.perform(get("/api/v1/community/posts/1/comments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].postId").value(1));
    }

    @Test
    @DisplayName("prometheus 엔드포인트는 accessToken 없이 접근할 수 있다")
    void prometheusEndpoint_isAccessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/actuator/prometheus"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("관리자 웹 origin은 CORS preflight를 통과한다")
    void corsPreflight_allowsAdminOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://admin.rolling-app.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "https://admin.rolling-app.com"));
    }

    @Test
    @DisplayName("허용되지 않은 origin은 CORS preflight를 통과하지 못한다")
    void corsPreflight_rejectsDisallowedOrigin() throws Exception {
        mockMvc.perform(options("/api/v1/auth/login")
                        .header(HttpHeaders.ORIGIN, "https://evil.example.com")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("prometheus 외 actuator 엔드포인트는 관리자 accessToken이 필요하다")
    void nonPrometheusActuatorEndpoints_requireAdminToken() throws Exception {
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/actuator/info")
                        .header("Authorization", bearerToken(2L)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/actuator/info")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("requestId 헤더가 없으면 서버가 새 값을 발급한다")
    void requestId_isGeneratedWhenMissing() throws Exception {
        stubNoticeResponses();

        mockMvc.perform(get("/api/v1/notices/1"))
                .andExpect(status().isOk())
                .andExpect(header().string(RequestTrackingFilter.REQUEST_ID_HEADER, not(isEmptyOrNullString())))
                .andExpect(header().string(RequestTrackingFilter.TRACE_ID_HEADER, not(isEmptyOrNullString())));
    }

    @Test
    @DisplayName("외부 requestId 헤더가 있으면 같은 값을 응답 헤더로 유지한다")
    void requestId_reusesIncomingHeader() throws Exception {
        stubNoticeResponses();

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

        mockMvc.perform(get("/api/v1/seminars/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/seminars/my-applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("계정 기반 액션은 accessToken 없이 접근할 수 없다")
    void accountBasedActions_requireAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/open-mats/11/apply"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/seminars/11/applications"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/seminars/11/reports")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"reason\": \"SPAM\"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/open-mats/11/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"reason\": \"SPAM\"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/tournaments/11/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"reason\": \"SPAM\"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/tournaments/11/favorite"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/tournaments/favorites"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(patch("/api/v1/tournaments/11/favorite-reminder")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"notificationEnabled\": true,
                                  \"remindDate\": \"2026-04-01\",
                                  \"remindTime\": \"09:00\"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/maps/kakao/geocode")
                        .param("address", "경남 창녕군 창녕읍 종로 2"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/community/posts/11/like"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/community/posts/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(post("/api/v1/community/posts/11/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"reason\": \"SPAM\"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/v1/admin/community/posts"))
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
    @DisplayName("카카오 주소 좌표 변환은 인증 후 address 파라미터를 요구한다")
    void geocode_withUserTokenAndMissingAddress_returnsValidationError() throws Exception {
        mockMvc.perform(get("/api/v1/maps/kakao/geocode")
                        .header("Authorization", bearerToken(2L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
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

        mockMvc.perform(patch("/api/v1/admin/community/posts/11/hide")
                        .header("Authorization", bearerToken(2L)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/admin/training-logs/comments/reports")
                        .header("Authorization", bearerToken(2L)))
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

        given(communityService.findPostsForAdmin(isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        CommunityAdminPostResponse.builder()
                                .id(1L)
                                .category(CommunityPostCategory.FREE)
                                .title("커뮤니티 글")
                                .content("본문")
                                .authorId(2L)
                                .authorNickname("community-user")
                                .status(CommunityPostStatus.ACTIVE)
                                .build()
                ), PageRequest.of(0, 20), 1));
        given(communityService.hidePost(1L, 1L)).willReturn(CommunityAdminPostResponse.builder()
                .id(1L)
                .status(CommunityPostStatus.HIDDEN)
                .build());
        given(communityService.findCommentsForAdmin(isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        CommunityAdminCommentResponse.builder()
                                .id(2L)
                                .postId(1L)
                                .postTitle("커뮤니티 글")
                                .authorId(3L)
                                .authorNickname("comment-user")
                                .status(com.rolling.api.domain.community.entity.CommunityCommentStatus.ACTIVE)
                                .build()
                ), PageRequest.of(0, 20), 1));
        given(communityService.findPostReportsForAdmin(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        CommunityPostReportAdminResponse.builder()
                                .id(3L)
                                .postId(1L)
                                .postTitle("커뮤니티 글")
                                .postAuthorNickname("community-user")
                                .reporterUserId(4L)
                                .reporterNickname("reporter")
                                .status(ReportStatus.RECEIVED)
                                .build()
                ), PageRequest.of(0, 20), 1));
        given(communityService.findCommentReportsForAdmin(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        CommunityCommentReportAdminResponse.builder()
                                .id(4L)
                                .commentId(2L)
                                .postId(1L)
                                .postTitle("커뮤니티 글")
                                .commentContent("댓글")
                                .commentAuthorNickname("comment-user")
                                .reporterUserId(5L)
                                .reporterNickname("reporter")
                                .status(ReportStatus.RECEIVED)
                                .build()
                ), PageRequest.of(0, 20), 1));
        given(communityService.updatePostReportStatus(eq(1L), eq(3L), any())).willReturn(CommunityPostReportAdminResponse.builder()
                .id(3L)
                .status(ReportStatus.RESOLVED)
                .build());
        given(trainingLogSocialService.findCommentReportsForAdmin(isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        TrainingLogCommentReportAdminResponse.builder()
                                .id(7L)
                                .commentId(11L)
                                .entryId(21L)
                                .entryTitle("훈련일지")
                                .commentAuthorNickname("commenter")
                                .reporterNickname("reporter")
                                .status(ReportStatus.RECEIVED)
                                .build()
                ), PageRequest.of(0, 20), 1));
        given(trainingLogSocialService.updateCommentReportStatus(eq(1L), eq(7L), any()))
                .willReturn(TrainingLogCommentReportAdminResponse.builder()
                        .id(7L)
                        .status(ReportStatus.RESOLVED)
                        .finalAction("CONTENT_HIDDEN")
                        .build());

        mockMvc.perform(get("/api/v1/admin/community/posts")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(1));

        mockMvc.perform(patch("/api/v1/admin/community/posts/1/hide")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("HIDDEN"));

        mockMvc.perform(get("/api/v1/admin/community/comments")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(2));

        mockMvc.perform(get("/api/v1/admin/community/posts/reports")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(3));

        mockMvc.perform(get("/api/v1/admin/community/comments/reports")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(4));

        mockMvc.perform(patch("/api/v1/admin/community/posts/reports/3/status")
                        .header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"status\": \"RESOLVED\",
                                  \"processingMemo\": \"검토 완료\",
                                  \"finalAction\": \"CONTENT_HIDDEN\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));

        mockMvc.perform(get("/api/v1/admin/training-logs/comments/reports")
                        .header("Authorization", bearerToken(1L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].id").value(7));

        mockMvc.perform(patch("/api/v1/admin/training-logs/comments/reports/7/status")
                        .header("Authorization", bearerToken(1L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"status\": \"RESOLVED\",
                                  \"processingMemo\": \"검토 완료\",
                                  \"finalAction\": \"CONTENT_HIDDEN\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RESOLVED"));
    }

    @Test
    @DisplayName("인증 사용자는 커뮤니티 좋아요와 신고 API에 접근할 수 있다")
    void communityActions_withUserToken_returnsOk() throws Exception {
        given(communityService.findMyPosts(eq(2L), isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        CommunityPostSummaryResponse.builder()
                                .id(12L)
                                .category(CommunityPostCategory.FREE)
                                .title("내 글")
                                .authorNickname("community-user")
                                .commentCount(0L)
                                .viewCount(1L)
                                .createdAt(LocalDateTime.of(2026, 3, 20, 11, 0))
                                .build()
                ), PageRequest.of(0, 20), 1));

        mockMvc.perform(post("/api/v1/community/posts/11/like")
                        .header("Authorization", bearerToken(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/v1/community/posts/my")
                        .header("Authorization", bearerToken(2L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(12));

        mockMvc.perform(post("/api/v1/community/posts/11/report")
                        .header("Authorization", bearerToken(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"reason\": \"SPAM\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/community/comments/11/report")
                        .header("Authorization", bearerToken(2L))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  \"reason\": \"INAPPROPRIATE\"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("제재 계정 accessToken은 비허용 API에서 403을 반환한다")
    void suspendedUserToken_onRestrictedRoute_returnsForbidden() throws Exception {
        User suspendedUser = User.builder()
                .socialId("social-suspended")
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("정지사용자")
                .email("suspended@example.com")
                .beltColor(BeltColor.WHITE)
                .build();
        org.springframework.test.util.ReflectionTestUtils.setField(suspendedUser, "id", 3L);
        suspendedUser.suspend(LocalDateTime.of(2126, 4, 20, 0, 0), "중대한 운영 위반");
        given(userRepository.findByIdAndIsWithdrawnFalse(3L)).willReturn(Optional.of(suspendedUser));

        mockMvc.perform(post("/api/v1/community/posts/11/like")
                        .header("Authorization", bearerToken(3L)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"));
    }

    private void stubOpenMatResponses() {
        OpenMatResponse response = OpenMatResponse.builder()
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
                .build();

        given(openMatService.findAll(isNull(), isNull(), isNull(), any(Pageable.class), isNull()))
                .willReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));
        given(openMatService.findById(eq(1L), isNull(), eq(false))).willReturn(response);
    }

    private void stubSeminarResponses() {
        SeminarResponse response = SeminarResponse.builder()
                .id(1L)
                .title("가드 패스 세미나")
                .description("가드 패스 기본기")
                .instructorName("김코치")
                .startDateTime(LocalDateTime.of(2026, 3, 22, 14, 0))
                .endDateTime(LocalDateTime.of(2026, 3, 22, 17, 0))
                .locationName("Rolling Gym")
                .address("Seoul")
                .region(Region.SEOUL)
                .maxCapacity(20)
                .appliedCount(2)
                .remainingCapacity(18)
                .price(30000)
                .status(SeminarStatus.RECRUITING)
                .reported(false)
                .hostId(1L)
                .hostNickname("host")
                .deleted(false)
                .createdAt(LocalDateTime.of(2026, 3, 20, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 20, 9, 0))
                .build();

        given(seminarService.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class), isNull()))
                .willReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));
        given(seminarService.findById(eq(1L), isNull())).willReturn(response);
    }

    private void stubTournamentResponses() {
        TournamentResponse response = TournamentResponse.builder()
                .id(1L)
                .source(TournamentSource.MANUAL)
                .title("국내 대회")
                .organizer("Rolling")
                .posterUrl("https://cdn.example.com/tournaments/1.jpg")
                .competitionDate(LocalDate.of(2026, 4, 5))
                .registrationDeadline(LocalDate.of(2026, 3, 31))
                .location("Seoul")
                .region(Region.SEOUL)
                .applyLink("https://example.com/apply")
                .registrationClosed(false)
                .createdAt(LocalDateTime.of(2026, 3, 20, 9, 0))
                .build();

        given(tournamentService.findAll(any(Pageable.class), isNull(), isNull(), isNull()))
                .willReturn(new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1));
        given(tournamentService.findById(eq(1L), isNull())).willReturn(response);
    }

    private void stubNoticeResponses() {
        given(noticeService.findAll(any(Pageable.class))).willReturn(new PageImpl<>(List.of(
                NoticeListItemResponse.builder()
                        .id(1L)
                        .title("공지")
                        .content("본문")
                        .authorName("Rolling Admin")
                        .createdAt(LocalDateTime.of(2026, 3, 20, 10, 0))
                        .build()
        ), PageRequest.of(0, 20), 1));
        given(noticeService.findById(1L)).willReturn(NoticeResponse.builder()
                .id(1L)
                .title("공지")
                .content("본문")
                .authorName("Rolling Admin")
                .createdAt(LocalDateTime.of(2026, 3, 20, 10, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 20, 10, 0))
                .build());
    }

    private void stubCommunityResponses() {
        given(communityService.findPosts(isNull(), isNull(), isNull(), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        CommunityPostSummaryResponse.builder()
                                .id(1L)
                                .category(CommunityPostCategory.FREE)
                                .title("커뮤니티 글")
                                .authorNickname("community-user")
                                .commentCount(2L)
                                .viewCount(10L)
                                .createdAt(LocalDateTime.of(2026, 3, 20, 11, 0))
                                .build()
                ), PageRequest.of(0, 20), 1));
        given(communityService.findPost(isNull(), eq(false), eq(1L))).willReturn(CommunityPostDetailResponse.builder()
                .id(1L)
                .category(CommunityPostCategory.FREE)
                .title("커뮤니티 글")
                .content("본문")
                .authorNickname("community-user")
                .commentCount(2L)
                .viewCount(10L)
                .editableByMe(false)
                .createdAt(LocalDateTime.of(2026, 3, 20, 11, 0))
                .updatedAt(LocalDateTime.of(2026, 3, 20, 11, 0))
                .build());
        given(communityService.findComments(isNull(), eq(false), eq(1L), any(Pageable.class)))
                .willReturn(new PageImpl<>(List.of(
                        CommunityCommentResponse.builder()
                                .id(1L)
                                .postId(1L)
                                .authorNickname("comment-user")
                                .content("댓글")
                                .createdAt(LocalDateTime.of(2026, 3, 20, 11, 30))
                                .updatedAt(LocalDateTime.of(2026, 3, 20, 11, 30))
                                .build()
                ), PageRequest.of(0, 20), 1));
    }

    private String bearerToken(Long userId) {
        return "Bearer " + jwtTokenProvider.createAccessToken(userId);
    }

    @Configuration
    @EnableWebMvc
    @EnableSpringDataWebSupport
    @Import({
            SecurityConfig.class,
            GlobalExceptionHandler.class,
            JwtTokenProvider.class,
            AdminAccessConfig.class,
            OpenMatController.class,
            SeminarController.class,
            TournamentController.class,
            NoticeController.class,
            NoticeAdminController.class,
            CommunityController.class,
            CommunityAdminController.class,
            TrainingLogAdminController.class,
            MapController.class,
            TournamentCrawlerController.class,
            TestActuatorController.class
    })
    static class TestConfig {

        @Bean
        OpenMatService openMatService() {
            return mock(OpenMatService.class);
        }

        @Bean
        SeminarService seminarService() {
            return mock(SeminarService.class);
        }

        @Bean
        NoticeService noticeService() {
            return mock(NoticeService.class);
        }

        @Bean
        CommunityService communityService() {
            return mock(CommunityService.class);
        }

        @Bean
        TrainingLogSocialService trainingLogSocialService() {
            return mock(TrainingLogSocialService.class);
        }

        @Bean
        CommunityPostImageUploadService communityPostImageUploadService() {
            return mock(CommunityPostImageUploadService.class);
        }

        @Bean
        TournamentService tournamentService() {
            return mock(TournamentService.class);
        }

        @Bean
        TournamentManagerService tournamentManagerService() {
            return mock(TournamentManagerService.class);
        }

        @Bean
        TournamentFavoriteService tournamentFavoriteService() {
            return mock(TournamentFavoriteService.class);
        }

        @Bean
        KakaoGeocodeService kakaoGeocodeService() {
            return mock(KakaoGeocodeService.class);
        }

        @Bean
        UserRepository userRepository() {
            return mock(UserRepository.class);
        }
    }

    @RestController
    static class TestActuatorController {

        @GetMapping("/actuator/prometheus")
        ResponseEntity<String> prometheus() {
            return ResponseEntity.ok("# test metric");
        }

        @GetMapping("/actuator/info")
        ResponseEntity<String> info() {
            return ResponseEntity.ok("{\"app\":\"rolling-api\"}");
        }
    }
}
