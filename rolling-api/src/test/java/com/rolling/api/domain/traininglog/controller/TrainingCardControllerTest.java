package com.rolling.api.domain.traininglog.controller;

import com.rolling.api.domain.traininglog.dto.TrainingCardDetailResponse;
import com.rolling.api.domain.traininglog.dto.TrainingCardListItemResponse;
import com.rolling.api.domain.traininglog.dto.TrainingCardRelatedItemResponse;
import com.rolling.api.domain.traininglog.entity.TrainingCardLevel;
import com.rolling.api.domain.traininglog.entity.TrainingCardPosition;
import com.rolling.api.domain.traininglog.service.TrainingCardService;
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
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TrainingCardControllerTest {

    private MockMvc mockMvc;
    private TrainingCardService trainingCardService;
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

        trainingCardService = context.getBean(TrainingCardService.class);
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
    @DisplayName("training card list requires authentication")
    void findCards_withoutAccessToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/training-logs/me/cards"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("UNAUTHORIZED"));
    }

    @Test
    @DisplayName("training card list accepts query level and position filters")
    void findCards_withFilters_returnsOk() throws Exception {
        authenticateUser();
        given(trainingCardService.findCards(2L, "knee", TrainingCardLevel.BEGINNER, TrainingCardPosition.GUARD))
                .willReturn(List.of(
                        TrainingCardListItemResponse.builder()
                                .id(1L)
                                .title("Knee Cut Pass")
                                .summary("상대 가드를 가로질러 압박으로 통과하는 패스")
                                .topic("PASS")
                                .level(TrainingCardLevel.BEGINNER)
                                .position(TrainingCardPosition.GUARD)
                                .situationSummary("니쉴드와 하프가드 압박 상황")
                                .likeCount(2L)
                                .likedByMe(true)
                                .favoritedByMe(false)
                                .build()
                ));

        mockMvc.perform(get("/api/v1/training-logs/me/cards")
                        .header("Authorization", "Bearer user-token")
                        .param("q", "knee")
                        .param("level", "BEGINNER")
                        .param("position", "GUARD"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title").value("Knee Cut Pass"))
                .andExpect(jsonPath("$.data[0].level").value("BEGINNER"))
                .andExpect(jsonPath("$.data[0].position").value("GUARD"))
                .andExpect(jsonPath("$.data[0].likeCount").value(2))
                .andExpect(jsonPath("$.data[0].likedByMe").value(true))
                .andExpect(jsonPath("$.data[0].favoritedByMe").value(false));
    }

    @Test
    @DisplayName("training card detail returns the full card description")
    void findCardDetail_withUserToken_returnsOk() throws Exception {
        authenticateUser();
        given(trainingCardService.findCardDetail(2L, 1L))
                .willReturn(TrainingCardDetailResponse.builder()
                        .id(1L)
                        .title("Knee Cut Pass")
                        .summary("상대 가드를 가로질러 압박으로 통과하는 패스")
                        .topic("PASS")
                        .level(TrainingCardLevel.BEGINNER)
                        .position(TrainingCardPosition.GUARD)
                        .situationSummary("니쉴드와 하프가드 압박 상황")
                        .description("무릎을 상대 다리 사이로 넣고 상체 압박으로 통과한다.")
                        .situationDescription("상대가 무릎 방패를 세우고 거리를 만들려고 할 때 사용한다.")
                        .startingPositionDescription("탑에서 한쪽 무릎이 상대 다리 라인을 넘볼 수 있는 거리")
                        .flowDescription("무릎 진입 -> 상체 압박 -> 반대 다리 정리 -> 사이드 고정")
                        .keyPoints("무릎 각도와 머리 위치를 잃지 않는다.")
                        .commonMistakes("상체 압박이 빠져 무릎만 먼저 들어간다.")
                        .cautions("상대 언더훅을 허용하지 않도록 주의한다.")
                        .youtubeUrl("https://www.youtube.com/watch?v=example")
                        .likeCount(4L)
                        .likedByMe(true)
                        .favoritedByMe(true)
                        .relatedCards(List.of(
                                TrainingCardRelatedItemResponse.builder()
                                        .id(2L)
                                        .title("Toreando Pass")
                                        .summary("양쪽 다리를 밀어내며 각도를 만들어 통과하는 패스")
                                        .topic("PASS")
                                        .level(TrainingCardLevel.INTERMEDIATE)
                                        .position(TrainingCardPosition.STANDING)
                                        .situationSummary("오픈가드에서 다리 라인을 치우는 상황")
                                        .build()
                        ))
                        .build());

        mockMvc.perform(get("/api/v1/training-logs/me/cards/1")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("Knee Cut Pass"))
                .andExpect(jsonPath("$.data.level").value("BEGINNER"))
                .andExpect(jsonPath("$.data.position").value("GUARD"))
                .andExpect(jsonPath("$.data.youtubeUrl").value("https://www.youtube.com/watch?v=example"))
                .andExpect(jsonPath("$.data.likeCount").value(4))
                .andExpect(jsonPath("$.data.likedByMe").value(true))
                .andExpect(jsonPath("$.data.favoritedByMe").value(true))
                .andExpect(jsonPath("$.data.relatedCards[0].id").value(2))
                .andExpect(jsonPath("$.data.relatedCards[0].title").value("Toreando Pass"));
    }

    @Test
    @DisplayName("training card like endpoint delegates to service")
    void likeCard_withUserToken_returnsOk() throws Exception {
        authenticateUser();

        mockMvc.perform(post("/api/v1/training-logs/me/cards/1/like")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(trainingCardService).likeCard(2L, 1L);
    }

    @Test
    @DisplayName("training card favorite delete endpoint delegates to service")
    void unfavoriteCard_withUserToken_returnsOk() throws Exception {
        authenticateUser();

        mockMvc.perform(delete("/api/v1/training-logs/me/cards/1/favorite")
                        .header("Authorization", "Bearer user-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(trainingCardService).unfavoriteCard(2L, 1L);
    }

    private void authenticateUser() {
        given(jwtTokenProvider.validateToken("user-token")).willReturn(true);
        given(jwtTokenProvider.getUserIdFromToken("user-token")).willReturn(2L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(2L)).willReturn(true);
        given(adminAccessConfig.isAdmin(2L)).willReturn(false);
    }

    @Configuration
    @EnableWebMvc
    @Import({TrainingCardController.class, GlobalExceptionHandler.class, SecurityConfig.class, SpringDataWebConfiguration.class})
    static class TestConfig {

        @Bean
        TrainingCardService trainingCardService() {
            return mock(TrainingCardService.class);
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
