package com.rolling.api.domain.traininglog.service;

import com.rolling.api.domain.traininglog.dto.TrainingLogAttendanceGrassDay;
import com.rolling.api.domain.traininglog.dto.TrainingLogAttendanceGrassResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogDailyInsight;
import com.rolling.api.domain.traininglog.dto.TrainingLogInsightPeriod;
import com.rolling.api.domain.traininglog.dto.TrainingLogInsightResponse;
import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogEntry;
import com.rolling.api.domain.traininglog.repository.TrainingLogEntryRepository;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class TrainingLogInsightServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private TrainingLogEntryRepository trainingLogEntryRepository;

    @Mock
    private UserRepository userRepository;

    private TrainingLogInsightService trainingLogInsightService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-17T03:00:00Z"), SEOUL_ZONE);
        trainingLogInsightService = new TrainingLogInsightService(
                trainingLogEntryRepository,
                userRepository,
                fixedClock
        );
    }

    @Test
    @DisplayName("attendance grass returns 365 days with levels and streaks")
    void getAttendanceGrass_returnsGrassDaysWithLevelsAndStreaks() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingLogEntryRepository.findAllByUser_IdAndTrainingDateGreaterThanEqualAndTrainingDateLessThanOrderByTrainingDateAscCreatedAtAsc(
                10L,
                LocalDate.of(2025, 5, 18),
                LocalDate.of(2026, 5, 18)
        )).willReturn(List.of(
                createEntryWithAttendance(1L, createUser(10L), LocalDate.of(2026, 5, 14), true, 30, 3, 2),
                createEntryWithAttendance(2L, createUser(10L), LocalDate.of(2026, 5, 15), true, 90, 4, 3),
                createEntryWithAttendance(3L, createUser(10L), LocalDate.of(2026, 5, 16), false, 120, 5, 4),
                createEntryWithAttendance(4L, createUser(10L), LocalDate.of(2026, 5, 17), true, 120, 5, 4),
                createEntryWithAttendance(5L, createUser(10L), LocalDate.of(2026, 5, 17), false, null, null, null)
        ));

        TrainingLogAttendanceGrassResponse response = trainingLogInsightService.getAttendanceGrass(10L, LocalDate.of(2026, 5, 17));

        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2025, 5, 18));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 17));
        assertThat(response.getTotalDays()).isEqualTo(365);
        assertThat(response.getDays()).hasSize(365);
        assertThat(response.getAttendanceDays()).isEqualTo(3);
        assertThat(response.getCurrentStreakDays()).isEqualTo(1);
        assertThat(response.getLongestStreakDays()).isEqualTo(2);
        assertThat(response.getRecent30DaysAttendanceDays()).isEqualTo(3);

        assertThat(response.getDays().get(0).date()).isEqualTo(LocalDate.of(2025, 5, 18));
        assertThat(response.getDays().get(0).dayOfWeek()).isEqualTo("SUN");
        assertThat(response.getDays().get(0).attended()).isFalse();
        assertThat(response.getDays().get(0).level()).isZero();

        assertThat(findGrassDay(response, LocalDate.of(2026, 5, 14)).level()).isEqualTo(1);
        assertThat(findGrassDay(response, LocalDate.of(2026, 5, 15)).level()).isEqualTo(2);
        assertThat(findGrassDay(response, LocalDate.of(2026, 5, 16)).attended()).isFalse();
        assertThat(findGrassDay(response, LocalDate.of(2026, 5, 16)).level()).isZero();
        assertThat(findGrassDay(response, LocalDate.of(2026, 5, 17)).level()).isEqualTo(3);
        assertThat(findGrassDay(response, LocalDate.of(2026, 5, 17)).recordCount()).isEqualTo(2);
        assertThat(findGrassDay(response, LocalDate.of(2026, 5, 17)).totalTrainingMinutes()).isEqualTo(120);
        assertThat(findGrassDay(response, LocalDate.of(2026, 5, 17)).averageTrainingIntensity()).isEqualTo(5.0);
        assertThat(findGrassDay(response, LocalDate.of(2026, 5, 17)).averageCondition()).isEqualTo(4.0);
    }

    @Test
    @DisplayName("attendance grass uses today when reference date is omitted")
    void getAttendanceGrass_withoutReferenceDate_usesToday() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingLogEntryRepository.findAllByUser_IdAndTrainingDateGreaterThanEqualAndTrainingDateLessThanOrderByTrainingDateAscCreatedAtAsc(
                10L,
                LocalDate.of(2025, 5, 18),
                LocalDate.of(2026, 5, 18)
        )).willReturn(List.of());

        TrainingLogAttendanceGrassResponse response = trainingLogInsightService.getAttendanceGrass(10L, null);

        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2025, 5, 18));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 17));
        assertThat(response.getDays()).hasSize(365);
        assertThat(response.getAttendanceDays()).isZero();
    }

    @Test
    @DisplayName("insight period dates use week and month boundaries")
    void calculateInsightPeriodDates_usesWeekAndMonthBoundaries() {
        assertThat(trainingLogInsightService.calculateInsightStartDate(TrainingLogInsightPeriod.WEEK, LocalDate.of(2026, 5, 22)))
                .isEqualTo(LocalDate.of(2026, 5, 18));
        assertThat(trainingLogInsightService.calculateInsightEndDate(TrainingLogInsightPeriod.WEEK, LocalDate.of(2026, 5, 22)))
                .isEqualTo(LocalDate.of(2026, 5, 24));
        assertThat(trainingLogInsightService.calculateInsightStartDate(TrainingLogInsightPeriod.MONTH, LocalDate.of(2026, 5, 22)))
                .isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(trainingLogInsightService.calculateInsightEndDate(TrainingLogInsightPeriod.MONTH, LocalDate.of(2026, 5, 22)))
                .isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    @DisplayName("insight period dates use today when reference date is omitted")
    void calculateInsightPeriodDates_withoutReferenceDate_usesToday() {
        assertThat(trainingLogInsightService.calculateInsightStartDate(TrainingLogInsightPeriod.WEEK, null))
                .isEqualTo(LocalDate.of(2026, 5, 11));
        assertThat(trainingLogInsightService.calculateInsightEndDate(TrainingLogInsightPeriod.WEEK, null))
                .isEqualTo(LocalDate.of(2026, 5, 17));
    }

    @Test
    @DisplayName("weekly insights aggregate summary, daily stats, categories, and hashtags")
    void getInsights_weekly_returnsAggregatedInsights() {
        User user = createUser(10L);
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingLogEntryRepository.findAllByUser_IdAndTrainingDateGreaterThanEqualAndTrainingDateLessThanOrderByTrainingDateAscCreatedAtAsc(
                10L,
                LocalDate.of(2026, 5, 18),
                LocalDate.of(2026, 5, 25)
        )).willReturn(List.of(
                createInsightEntry(
                        1L,
                        user,
                        LocalDate.of(2026, 5, 18),
                        TrainingLogCategory.TECHNIQUE,
                        true,
                        60,
                        4,
                        3,
                        """
                                [
                                  {"text":"guard pass","checked":true,"favorite":false},
                                  {"text":"triangle","checked":false,"favorite":false}
                                ]
                                """,
                        """
                                ["guard-pass","triangle"]
                                """
                ),
                createInsightEntry(
                        2L,
                        user,
                        LocalDate.of(2026, 5, 18),
                        TrainingLogCategory.SPARRING,
                        false,
                        null,
                        null,
                        null,
                        null,
                        """
                                ["guard-pass"]
                                """
                ),
                createInsightEntry(
                        3L,
                        user,
                        LocalDate.of(2026, 5, 20),
                        TrainingLogCategory.TECHNIQUE,
                        true,
                        120,
                        2,
                        5,
                        """
                                [{"text":"review","checked":true,"favorite":false}]
                                """,
                        """
                                ["armbar"]
                                """
                ),
                createInsightEntry(
                        4L,
                        user,
                        LocalDate.of(2026, 5, 22),
                        TrainingLogCategory.DRILL,
                        false,
                        30,
                        5,
                        null,
                        null,
                        null
                )
        ));

        TrainingLogInsightResponse response = trainingLogInsightService.getInsights(
                10L,
                TrainingLogInsightPeriod.WEEK,
                LocalDate.of(2026, 5, 22)
        );

        assertThat(response.getPeriod()).isEqualTo(TrainingLogInsightPeriod.WEEK);
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 18));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 24));
        assertThat(response.getDailyStats()).hasSize(7);

        assertThat(response.getSummary().getRecordCount()).isEqualTo(4);
        assertThat(response.getSummary().getTrainingDays()).isEqualTo(3);
        assertThat(response.getSummary().getAttendanceDays()).isEqualTo(2);
        assertThat(response.getSummary().getAttendanceRate()).isEqualTo(28.6);
        assertThat(response.getSummary().getTotalTrainingMinutes()).isEqualTo(210);
        assertThat(response.getSummary().getAverageTrainingMinutesPerAttendanceDay()).isEqualTo(105.0);
        assertThat(response.getSummary().getAverageTrainingIntensity()).isEqualTo(3.7);
        assertThat(response.getSummary().getAverageCondition()).isEqualTo(4.0);
        assertThat(response.getSummary().getChecklistCompletionRate()).isEqualTo(66.7);

        TrainingLogDailyInsight monday = response.getDailyStats().get(0);
        assertThat(monday.date()).isEqualTo(LocalDate.of(2026, 5, 18));
        assertThat(monday.recordCount()).isEqualTo(2);
        assertThat(monday.gymAttendance()).isTrue();
        assertThat(monday.totalTrainingMinutes()).isEqualTo(60);
        assertThat(monday.averageTrainingIntensity()).isEqualTo(4.0);
        assertThat(monday.averageCondition()).isEqualTo(3.0);
        assertThat(monday.categories()).containsExactly(TrainingLogCategory.TECHNIQUE, TrainingLogCategory.SPARRING);

        TrainingLogDailyInsight tuesday = response.getDailyStats().get(1);
        assertThat(tuesday.date()).isEqualTo(LocalDate.of(2026, 5, 19));
        assertThat(tuesday.recordCount()).isZero();
        assertThat(tuesday.gymAttendance()).isFalse();
        assertThat(tuesday.totalTrainingMinutes()).isZero();
        assertThat(tuesday.averageTrainingIntensity()).isNull();
        assertThat(tuesday.averageCondition()).isNull();
        assertThat(tuesday.categories()).isEmpty();

        assertThat(response.getCategoryBreakdown())
                .extracting("category", "recordCount", "totalTrainingMinutes")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(TrainingLogCategory.TECHNIQUE, 2, 180),
                        org.assertj.core.groups.Tuple.tuple(TrainingLogCategory.SPARRING, 1, 0),
                        org.assertj.core.groups.Tuple.tuple(TrainingLogCategory.DRILL, 1, 30)
                );
        assertThat(response.getTopHashtags())
                .extracting("tag", "count")
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("guard-pass", 2),
                        org.assertj.core.groups.Tuple.tuple("triangle", 1),
                        org.assertj.core.groups.Tuple.tuple("armbar", 1)
                );
    }

    @Test
    @DisplayName("monthly insights use full month and return null averages when there is no source value")
    void getInsights_monthly_returnsEmptyMonthStats() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);
        given(trainingLogEntryRepository.findAllByUser_IdAndTrainingDateGreaterThanEqualAndTrainingDateLessThanOrderByTrainingDateAscCreatedAtAsc(
                10L,
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 6, 1)
        )).willReturn(List.of());

        TrainingLogInsightResponse response = trainingLogInsightService.getInsights(
                10L,
                TrainingLogInsightPeriod.MONTH,
                LocalDate.of(2026, 5, 22)
        );

        assertThat(response.getPeriod()).isEqualTo(TrainingLogInsightPeriod.MONTH);
        assertThat(response.getStartDate()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(response.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 31));
        assertThat(response.getDailyStats()).hasSize(31);
        assertThat(response.getSummary().getRecordCount()).isZero();
        assertThat(response.getSummary().getTrainingDays()).isZero();
        assertThat(response.getSummary().getAttendanceDays()).isZero();
        assertThat(response.getSummary().getAttendanceRate()).isEqualTo(0.0);
        assertThat(response.getSummary().getTotalTrainingMinutes()).isZero();
        assertThat(response.getSummary().getAverageTrainingMinutesPerAttendanceDay()).isNull();
        assertThat(response.getSummary().getAverageTrainingIntensity()).isNull();
        assertThat(response.getSummary().getAverageCondition()).isNull();
        assertThat(response.getSummary().getChecklistCompletionRate()).isNull();
        assertThat(response.getCategoryBreakdown()).isEmpty();
        assertThat(response.getTopHashtags()).isEmpty();
    }

    @Test
    @DisplayName("insights require period")
    void getInsights_withoutPeriod_throwsBadRequest() {
        given(userRepository.existsByIdAndIsWithdrawnFalse(10L)).willReturn(true);

        assertThatThrownBy(() -> trainingLogInsightService.getInsights(10L, null, LocalDate.of(2026, 5, 22)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("period는 필수입니다");
    }

    private User createUser(Long id) {
        User user = User.builder()
                .socialId("user-" + id)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("user-" + id)
                .email("user-" + id + "@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private TrainingLogEntry createEntryWithAttendance(
            Long id,
            User user,
            LocalDate trainingDate,
            Boolean gymAttendance,
            Integer trainingMinutes,
            Integer trainingIntensity,
            Integer condition
    ) {
        TrainingLogEntry entry = TrainingLogEntry.builder()
                .user(user)
                .trainingDate(trainingDate)
                .category(TrainingLogCategory.TECHNIQUE)
                .title("Training " + trainingDate)
                .content("Training note " + trainingDate)
                .gymAttendance(gymAttendance)
                .trainingMinutes(trainingMinutes)
                .trainingIntensity(trainingIntensity)
                .condition(condition)
                .build();
        ReflectionTestUtils.setField(entry, "id", id);
        ReflectionTestUtils.setField(entry, "createdAt", LocalDateTime.of(2026, 5, 17, 9, 0));
        ReflectionTestUtils.setField(entry, "updatedAt", LocalDateTime.of(2026, 5, 17, 9, 0));
        return entry;
    }

    private TrainingLogEntry createInsightEntry(
            Long id,
            User user,
            LocalDate trainingDate,
            TrainingLogCategory category,
            Boolean gymAttendance,
            Integer trainingMinutes,
            Integer trainingIntensity,
            Integer condition,
            String checklistJson,
            String hashtagsJson
    ) {
        TrainingLogEntry entry = TrainingLogEntry.builder()
                .user(user)
                .trainingDate(trainingDate)
                .category(category)
                .title("Training " + trainingDate)
                .content("Training note " + trainingDate)
                .gymAttendance(gymAttendance)
                .trainingMinutes(trainingMinutes)
                .trainingIntensity(trainingIntensity)
                .condition(condition)
                .checklistJson(checklistJson)
                .hashtagsJson(hashtagsJson)
                .build();
        ReflectionTestUtils.setField(entry, "id", id);
        ReflectionTestUtils.setField(entry, "createdAt", LocalDateTime.of(2026, 5, 17, 9, id.intValue()));
        ReflectionTestUtils.setField(entry, "updatedAt", LocalDateTime.of(2026, 5, 17, 9, id.intValue()));
        return entry;
    }

    private TrainingLogAttendanceGrassDay findGrassDay(
            TrainingLogAttendanceGrassResponse response,
            LocalDate date
    ) {
        return response.getDays().stream()
                .filter(day -> day.date().equals(date))
                .findFirst()
                .orElseThrow();
    }
}
