package com.rolling.api.domain.tournament.controller;

import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.tournament.dto.TournamentResponse;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.tournament.service.TournamentService;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.config.SecurityConfig;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.exception.GlobalExceptionHandler;
import com.rolling.api.global.security.AdminAccessConfig;
import com.rolling.api.global.security.UserPrincipal;
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
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TournamentControllerTest {

    private MockMvc mockMvc;
    private TournamentService tournamentService;
    private JwtTokenProvider jwtTokenProvider;
    private UserRepository userRepository;
    private AdminAccessConfig adminAccessConfig;
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

        tournamentService = context.getBean(TournamentService.class);
        jwtTokenProvider = context.getBean(JwtTokenProvider.class);
        userRepository = context.getBean(UserRepository.class);
        adminAccessConfig = context.getBean(AdminAccessConfig.class);
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
    @DisplayName("대회 상세 조회는 인증 없이 접근할 수 있다")
    void findById_isPublic() throws Exception {
        given(tournamentService.findById(10L, null)).willReturn(response(10L));

        mockMvc.perform(get("/api/v1/tournaments/10").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.posterUrl").value("https://cdn.rolling.com/posters/10.jpg"))
                .andExpect(jsonPath("$.data.title").value("제5회 롤링컵"));

        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);
        given(tournamentService.findById(10L, 2L)).willReturn(response(10L));

        mockMvc.perform(get("/api/v1/tournaments/10")
                        .header("Authorization", "Bearer user-token")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.posterUrl").value("https://cdn.rolling.com/posters/10.jpg"));
    }

    @Test
    @DisplayName("대회 목록 조회는 인증 사용자의 viewer id를 서비스에 전달한다")
    void list_withUserToken_passesViewerId() {
        TournamentController controller = new TournamentController(tournamentService);
        PageRequest pageable = PageRequest.of(0, 20);
        given(tournamentService.findAll(pageable, TournamentSource.MANUAL, 2L))
                .willReturn(new org.springframework.data.domain.PageImpl<>(java.util.List.of(response(10L))));

        controller.list(new UserPrincipal(2L), TournamentSource.MANUAL, pageable);

        verify(tournamentService).findAll(pageable, TournamentSource.MANUAL, 2L);
    }

    @Test
    @DisplayName("대회 신고는 인증 사용자가 성공적으로 요청할 수 있다")
    void report_withUserToken_returnsOk() throws Exception {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);

        mockMvc.perform(post("/api/v1/tournaments/10/report")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "SPAM",
                                  "customReason": "광고성 게시물입니다"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(tournamentService).report(
                eq(2L),
                eq(10L),
                any(com.rolling.api.domain.report.dto.ReportCreateRequest.class)
        );
    }

    @Test
    @DisplayName("대회 신고는 이미 신고한 대상이면 ALREADY_REPORTED를 반환한다")
    void report_duplicate_returnsBadRequest() throws Exception {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);
        doThrow(new BusinessException("ALREADY_REPORTED", "이미 신고한 대상입니다", org.springframework.http.HttpStatus.BAD_REQUEST))
                .when(tournamentService).report(eq(2L), eq(10L), any());

        mockMvc.perform(post("/api/v1/tournaments/10/report")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "SPAM"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ALREADY_REPORTED"))
                .andExpect(jsonPath("$.error.message").value("이미 신고한 대상입니다"));
    }

    @Test
    @DisplayName("대회 신고는 자기 작성 대상이면 SELF_REPORT_NOT_ALLOWED를 반환한다")
    void report_self_returnsBadRequest() throws Exception {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);
        doThrow(new BusinessException("SELF_REPORT_NOT_ALLOWED", "자신이 작성한 게시글은 신고할 수 없습니다", org.springframework.http.HttpStatus.BAD_REQUEST))
                .when(tournamentService).report(eq(2L), eq(10L), any());

        mockMvc.perform(post("/api/v1/tournaments/10/report")
                        .header("Authorization", "Bearer user-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "SPAM"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SELF_REPORT_NOT_ALLOWED"))
                .andExpect(jsonPath("$.error.message").value("자신이 작성한 게시글은 신고할 수 없습니다"));
    }

    @Test
    @DisplayName("대회 신고는 accessToken이 없으면 401을 반환한다")
    void report_withoutAccessToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(post("/api/v1/tournaments/10/report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "SPAM"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    private TournamentResponse response(Long id) {
        return TournamentResponse.builder()
                .id(id)
                .source(TournamentSource.MANUAL)
                .title("제5회 롤링컵")
                .organizer("롤링 주짓수")
                .posterUrl("https://cdn.rolling.com/posters/10.jpg")
                .competitionDate(LocalDate.of(2026, 4, 15))
                .registrationDeadline(LocalDate.of(2026, 4, 1))
                .location("서울 올림픽공원 체조경기장")
                .applyLink("https://forms.google.com/rolling")
                .registrationClosed(false)
                .createdAt(LocalDateTime.of(2026, 4, 1, 12, 0))
                .build();
    }

    @Configuration
    @EnableWebMvc
    @Import({TournamentController.class, GlobalExceptionHandler.class, SecurityConfig.class})
    static class TestConfig {

        @Bean
        TournamentService tournamentService() {
            return mock(TournamentService.class);
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
