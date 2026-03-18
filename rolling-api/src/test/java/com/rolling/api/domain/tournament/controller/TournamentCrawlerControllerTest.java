package com.rolling.api.domain.tournament.controller;

import com.rolling.api.domain.openmat.config.OpenMatTestingAccessConfig;
import com.rolling.api.domain.tournament.dto.TournamentCrawlResult;
import com.rolling.api.domain.tournament.service.TournamentManagerService;
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
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TournamentCrawlerControllerTest {

    private MockMvc mockMvc;
    private TournamentManagerService tournamentManagerService;
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

        tournamentManagerService = context.getBean(TournamentManagerService.class);
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
    @DisplayName("대회 크롤링 실행은 admin key가 있으면 성공한다")
    void crawl_withAdminKey_returnsOk() throws Exception {
        TournamentCrawlResult result = TournamentCrawlResult.builder()
                .crawledCount(10)
                .createdCount(3)
                .updatedCount(5)
                .skippedCount(2)
                .build();

        given(adminAccessConfig.matchesAdminApiKey("crawler-secret")).willReturn(true);
        given(tournamentManagerService.crawlAndSaveAll()).willReturn(result);

        mockMvc.perform(post("/api/v1/tournaments/crawl")
                        .header("X-Crawler-Admin-Key", "crawler-secret"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.crawledCount").value(10))
                .andExpect(jsonPath("$.data.createdCount").value(3));
    }

    @Test
    @DisplayName("대회 크롤링 실행은 admin accessToken만으로는 통과할 수 없다")
    void crawl_withBearerAdminTokenOnly_returnsForbidden() throws Exception {
        given(jwtTokenProvider.validateToken("admin-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("admin-token")).willReturn(1L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(1L)).willReturn(true);
        given(adminAccessConfig.isAdmin(1L)).willReturn(true);

        mockMvc.perform(post("/api/v1/tournaments/crawl")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isForbidden());
    }

    @Configuration
    @EnableWebMvc
    @Import({TournamentCrawlerController.class, GlobalExceptionHandler.class, SecurityConfig.class})
    static class TestConfig {

        @Bean
        TournamentManagerService tournamentManagerService() {
            return mock(TournamentManagerService.class);
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
