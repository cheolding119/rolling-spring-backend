package com.rolling.api.domain.achievement;

import com.jayway.jsonpath.JsonPath;
import com.rolling.api.domain.achievement.repository.TournamentAchievementRepository;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.security.jwt.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        "spring.config.import=",
        "spring.datasource.url=jdbc:h2:mem:achievement-api;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "jwt.access-token-expiry=1800000", "jwt.refresh-token-expiry=1209600000",
        "spring.profiles.active=prod", "firebase.enabled=false",
        "openmat.status.schedule.enabled=false", "tournament.crawler.schedule.enabled=false",
        "cloud.aws.s3.bucket=test-bucket", "cloud.aws.s3.public-base-url=https://cdn.test.com",
        "cloud.aws.credentials.access-key=test", "cloud.aws.credentials.secret-key=test",
        "cloud.aws.region.static=ap-northeast-2"
})
@Transactional
class TournamentAchievementIntegrationTest {
    private static final String BASE = "/api/v1/tournament-achievements/me";
    private static final String GOLD = """
            {"tournamentName":"  Seoul Open  ","competitionDate":"2026-10-03","result":"GOLD",
             "uniformType":"GI","beltColor":"BLUE","ageDivision":"Adult","divisionType":"WEIGHT",
             "weightClass":"-76kg","organizer":"  Organizer  ","resultUrl":"https://example.com/results",
             "memo":"  first competition  "}
            """;
    private static final String PARTICIPATION = """
            {"tournamentName":"Seoul Open","competitionDate":"2026-10-02","result":"PARTICIPATION"}
            """;

    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired TournamentAchievementRepository achievements;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean Clock clock;
    private MockMvc mvc;
    private User owner;
    private User other;

    @BeforeEach
    void setUp() {
        when(clock.instant()).thenReturn(Instant.parse("2026-10-02T15:30:00Z"));
        when(clock.getZone()).thenReturn(ZoneId.of("UTC"));
        when(clock.withZone(ZoneId.of("Asia/Seoul"))).thenReturn(
                Clock.fixed(Instant.parse("2026-10-02T15:30:00Z"), ZoneId.of("Asia/Seoul")));
        owner = users.save(user("owner"));
        other = users.save(user("other"));
        when(tokens.validateToken("owner-token")).thenReturn(true);
        when(tokens.getUserIdFromToken("owner-token")).thenReturn(owner.getId());
        when(tokens.validateToken("other-token")).thenReturn(true);
        when(tokens.getUserIdFromToken("other-token")).thenReturn(other.getId());
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void createReadReplaceDelete_updatesSummaryAndKeepsHistoricalBeltIndependent() throws Exception {
        mvc.perform(asOwner(get(BASE + "/summary")))
                .andExpect(jsonPath("$.data.recordCount").value(0))
                .andExpect(jsonPath("$.data.medalCount").value(0));
        long id = create("owner-token", GOLD);
        mvc.perform(asOwner(get(BASE + "/" + id)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tournamentName").value("Seoul Open"))
                .andExpect(jsonPath("$.data.competitionDate").value("2026-10-03"))
                .andExpect(jsonPath("$.data.beltColor").value("BLUE"))
                .andExpect(jsonPath("$.data.organizer").value("Organizer"))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists());
        assertThat(owner.getBeltColor()).isEqualTo(BeltColor.WHITE);
        mvc.perform(asOwner(get(BASE + "/summary")))
                .andExpect(jsonPath("$.data.goldCount").value(1))
                .andExpect(jsonPath("$.data.medalCount").value(1));
        mvc.perform(asOwner(put(BASE + "/" + id)).contentType(MediaType.APPLICATION_JSON).content(PARTICIPATION))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.result").value("PARTICIPATION"))
                .andExpect(jsonPath("$.data.weightClass").doesNotExist())
                .andExpect(jsonPath("$.data.memo").doesNotExist());
        mvc.perform(asOwner(get(BASE + "/summary")))
                .andExpect(jsonPath("$.data.goldCount").value(0))
                .andExpect(jsonPath("$.data.participationCount").value(1));
        mvc.perform(asOwner(delete(BASE + "/" + id))).andExpect(status().isOk());
        mvc.perform(asOwner(get(BASE + "/" + id))).andExpect(status().isNotFound());
        mvc.perform(asOwner(get(BASE + "/summary"))).andExpect(jsonPath("$.data.recordCount").value(0));
    }

    @Test
    void listAndAggregate_areOwnerScopedAndAllowMultipleDivisions() throws Exception {
        create("owner-token", GOLD);
        long second = create("owner-token", GOLD.replace("GOLD", "SILVER").replace("GI", "NO_GI"));
        create("owner-token", GOLD.replace("GOLD", "BRONZE").replace("2026-10-03", "2026-10-01"));
        create("owner-token", PARTICIPATION);
        create("other-token", GOLD);
        mvc.perform(asOwner(get(BASE).param("size", "2")))
                .andExpect(jsonPath("$.data.page.totalElements").value(4))
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(second));
        mvc.perform(asOwner(get(BASE + "/summary")))
                .andExpect(jsonPath("$.data.recordCount").value(4))
                .andExpect(jsonPath("$.data.goldCount").value(1))
                .andExpect(jsonPath("$.data.silverCount").value(1))
                .andExpect(jsonPath("$.data.bronzeCount").value(1))
                .andExpect(jsonPath("$.data.medalCount").value(3))
                .andExpect(jsonPath("$.data.participationCount").value(1));
        achievements.deleteAllByUserId(owner.getId());
        assertThat(achievements.countResults(owner.getId())).isEmpty();
        assertThat(achievements.countResults(other.getId())).hasSize(1);
    }

    @Test
    void anotherOwnerCannotReadEditOrDelete() throws Exception {
        long id = create("other-token", GOLD);
        mvc.perform(asOwner(get(BASE + "/" + id))).andExpect(status().isNotFound());
        mvc.perform(asOwner(put(BASE + "/" + id)).contentType(MediaType.APPLICATION_JSON).content(GOLD))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
        mvc.perform(asOwner(delete(BASE + "/" + id))).andExpect(status().isNotFound());
        assertThat(achievements.findById(id)).isPresent();
    }

    @Test
    void everyRouteRequiresAuthentication() throws Exception {
        for (var request : new MockHttpServletRequestBuilder[]{get(BASE), get(BASE + "/summary"),
                get(BASE + "/1"), post(BASE), put(BASE + "/1"), delete(BASE + "/1")}) {
            mvc.perform(request.contentType(MediaType.APPLICATION_JSON).content(GOLD))
                    .andExpect(status().isUnauthorized());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"tournamentName\":\" \",\"competitionDate\":\"2026-10-03\",\"result\":\"GOLD\"}",
            "{\"tournamentName\":\"Open\",\"competitionDate\":\"2026-10-04\",\"result\":\"GOLD\"}",
            "{\"tournamentName\":\"Open\",\"competitionDate\":\"not-a-date\",\"result\":\"GOLD\"}",
            "{\"tournamentName\":\"Open\",\"competitionDate\":\"2026-10-03\",\"result\":\"INVALID\"}",
            "{\"tournamentName\":\"Open\",\"competitionDate\":\"2026-10-03\",\"result\":\"GOLD\",\"beltColor\":\"INVALID\"}",
            "{\"tournamentName\":\"Open\",\"competitionDate\":\"2026-10-03\",\"result\":\"GOLD\",\"resultUrl\":\"http://example.com\"}",
            "{\"tournamentName\":\"Open\",\"competitionDate\":\"2026-10-03\",\"result\":\"GOLD\",\"resultUrl\":\"https:///no-host\"}",
            "{\"tournamentName\":\"Open\",\"competitionDate\":\"2026-10-03\",\"result\":\"GOLD\",\"divisionType\":\"ABSOLUTE\",\"weightClass\":\"-76kg\"}"
    })
    void invalidInputReturns400AndDoesNotPersist(String json) throws Exception {
        mvc.perform(asOwner(post(BASE)).contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        assertThat(achievements.countResults(owner.getId())).isEmpty();
    }

    @Test
    void lengthAndPaginationLimitsAreEnforced() throws Exception {
        mvc.perform(asOwner(post(BASE)).contentType(MediaType.APPLICATION_JSON)
                        .content(GOLD.replace("Seoul Open", "A".repeat(256))))
                .andExpect(status().isBadRequest());
        mvc.perform(asOwner(get(BASE).param("page", "-1"))).andExpect(status().isBadRequest());
        mvc.perform(asOwner(get(BASE).param("size", "0"))).andExpect(status().isBadRequest());
        mvc.perform(asOwner(get(BASE).param("size", "101"))).andExpect(status().isBadRequest());
    }

    private long create(String token, String body) throws Exception {
        String response = mvc.perform(post(BASE).header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.parse(response).read("$.data.id", Long.class);
    }

    private MockHttpServletRequestBuilder asOwner(MockHttpServletRequestBuilder request) {
        return request.header("Authorization", "Bearer owner-token");
    }

    private User user(String socialId) {
        return User.builder().socialId(socialId).socialProvider(SocialProvider.GOOGLE)
                .nickname(socialId).beltColor(BeltColor.WHITE).build();
    }
}
