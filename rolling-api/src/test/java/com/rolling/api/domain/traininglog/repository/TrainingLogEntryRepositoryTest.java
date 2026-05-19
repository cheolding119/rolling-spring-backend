package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingLogCategory;
import com.rolling.api.domain.traininglog.entity.TrainingLogEntry;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:training-log-repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.flyway.enabled=false",
        "jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "jwt.access-token-expiry=1800000",
        "jwt.refresh-token-expiry=1209600000",
        "spring.profiles.active=prod",
        "firebase.enabled=false",
        "openmat.status.schedule.enabled=false",
        "tournament.crawler.schedule.enabled=false",
        "cloud.aws.s3.bucket=test-bucket",
        "cloud.aws.s3.public-base-url=https://cdn.test.com",
        "cloud.aws.credentials.access-key=test-access-key",
        "cloud.aws.credentials.secret-key=test-secret-key",
        "cloud.aws.region.static=ap-northeast-2"
})
class TrainingLogEntryRepositoryTest {

    @Autowired
    private TrainingLogEntryRepository trainingLogEntryRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("daily and monthly summary queries aggregate only the target user's entries")
    void summaryQueries_aggregateDailyAndMonthlyValues() {
        User owner = userRepository.save(createUser("owner"));
        User otherUser = userRepository.save(createUser("other"));

        trainingLogEntryRepository.save(createEntry(owner, LocalDate.of(2026, 5, 1), 60));
        trainingLogEntryRepository.save(createEntry(owner, LocalDate.of(2026, 5, 1), 90));
        trainingLogEntryRepository.save(createEntry(owner, LocalDate.of(2026, 5, 3), null));
        trainingLogEntryRepository.save(createEntry(owner, LocalDate.of(2026, 6, 1), 30));
        trainingLogEntryRepository.save(createEntry(otherUser, LocalDate.of(2026, 5, 1), 999));

        List<TrainingLogCalendarDailyProjection> dailySummaries =
                trainingLogEntryRepository.findDailySummariesByUserIdAndTrainingDateBetween(
                        owner.getId(),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2027, 1, 1)
                );
        List<TrainingLogCalendarMonthlyProjection> monthlySummaries =
                trainingLogEntryRepository.findMonthlySummariesByUserIdAndTrainingDateBetween(
                        owner.getId(),
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2027, 1, 1)
                );

        assertThat(dailySummaries).hasSize(3);
        assertThat(dailySummaries.get(0).date()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(dailySummaries.get(0).totalMinutes()).isEqualTo(150L);
        assertThat(dailySummaries.get(0).recordCount()).isEqualTo(2L);
        assertThat(dailySummaries.get(1).date()).isEqualTo(LocalDate.of(2026, 5, 3));
        assertThat(dailySummaries.get(1).totalMinutes()).isZero();
        assertThat(dailySummaries.get(1).recordCount()).isEqualTo(1L);

        assertThat(monthlySummaries).hasSize(2);
        assertThat(monthlySummaries.get(0).month()).isEqualTo(5);
        assertThat(monthlySummaries.get(0).totalMinutes()).isEqualTo(150L);
        assertThat(monthlySummaries.get(0).activeDays()).isEqualTo(2L);
        assertThat(monthlySummaries.get(1).month()).isEqualTo(6);
        assertThat(monthlySummaries.get(1).totalMinutes()).isEqualTo(30L);
        assertThat(monthlySummaries.get(1).activeDays()).isEqualTo(1L);
    }

    @Test
    @DisplayName("monthly range query returns only entries inside the target month in order")
    void monthlyRangeQuery_returnsOrderedEntriesForTargetMonth() {
        User owner = userRepository.save(createUser("owner-monthly"));
        User otherUser = userRepository.save(createUser("other-monthly"));

        TrainingLogEntry first = trainingLogEntryRepository.save(createEntry(owner, LocalDate.of(2026, 5, 1), 60));
        TrainingLogEntry second = trainingLogEntryRepository.save(createEntry(owner, LocalDate.of(2026, 5, 3), 30));
        trainingLogEntryRepository.save(createEntry(owner, LocalDate.of(2026, 6, 1), 45));
        trainingLogEntryRepository.save(createEntry(otherUser, LocalDate.of(2026, 5, 1), 999));

        List<TrainingLogEntry> mayEntries = trainingLogEntryRepository
                .findAllByUser_IdAndTrainingDateGreaterThanEqualAndTrainingDateLessThanOrderByTrainingDateAscCreatedAtAsc(
                        owner.getId(),
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 6, 1)
                );

        assertThat(mayEntries).extracting(TrainingLogEntry::getId).containsExactly(first.getId(), second.getId());
        assertThat(mayEntries).allSatisfy(entry -> assertThat(entry.getTrainingDate().getMonthValue()).isEqualTo(5));
    }

    @Test
    @DisplayName("recent entry query respects page size and descending training date order")
    void findRecentEntries_returnsPagedNewestEntries() {
        User owner = userRepository.save(createUser("owner-recent"));

        for (int day = 1; day <= 12; day++) {
            trainingLogEntryRepository.save(createEntry(owner, LocalDate.of(2026, 5, day), day * 10));
        }

        List<TrainingLogEntry> recentEntries = trainingLogEntryRepository
                .findAllByUser_IdOrderByTrainingDateDescCreatedAtDesc(owner.getId(), PageRequest.of(0, 10));

        assertThat(recentEntries).hasSize(10);
        assertThat(recentEntries.get(0).getTrainingDate()).isEqualTo(LocalDate.of(2026, 5, 12));
        assertThat(recentEntries.get(9).getTrainingDate()).isEqualTo(LocalDate.of(2026, 5, 3));
    }

    @Test
    @DisplayName("latest promotion query returns the newest promotion entry")
    void findLatestPromotionEntry_returnsNewestPromotion() {
        User owner = userRepository.save(createUser("owner-promotion"));

        TrainingLogEntry olderPromotion = trainingLogEntryRepository.save(createPromotionEntry(owner, LocalDate.of(2026, 5, 1), 1, BeltColor.BLUE));
        TrainingLogEntry newerPromotion = trainingLogEntryRepository.save(createPromotionEntry(owner, LocalDate.of(2026, 5, 10), 2, BeltColor.PURPLE));
        trainingLogEntryRepository.save(createEntry(owner, LocalDate.of(2026, 5, 15), 30));

        Optional<TrainingLogEntry> latestPromotion = trainingLogEntryRepository
                .findFirstByUser_IdAndCategoryOrderByTrainingDateDescCreatedAtDescIdDesc(owner.getId(), TrainingLogCategory.PROMOTION);

        assertThat(latestPromotion).isPresent();
        assertThat(latestPromotion.get().getId()).isEqualTo(newerPromotion.getId());
        assertThat(latestPromotion.get().getBeltColor()).isEqualTo(BeltColor.PURPLE);
    }

    private User createUser(String socialId) {
        return User.builder()
                .socialId(socialId)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname(socialId)
                .email(socialId + "@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
    }

    private TrainingLogEntry createEntry(User user, LocalDate trainingDate, Integer trainingMinutes) {
        return TrainingLogEntry.builder()
                .user(user)
                .trainingDate(trainingDate)
                .category(TrainingLogCategory.TECHNIQUE)
                .title("Training " + trainingDate)
                .content("Training note " + trainingDate)
                .trainingMinutes(trainingMinutes)
                .build();
    }

    private TrainingLogEntry createPromotionEntry(User user, LocalDate trainingDate, Integer stripeCount, BeltColor beltColor) {
        return TrainingLogEntry.builder()
                .user(user)
                .trainingDate(trainingDate)
                .category(TrainingLogCategory.PROMOTION)
                .title("Promotion " + trainingDate)
                .content("Promotion note " + trainingDate)
                .stripeCount(stripeCount)
                .beltColor(beltColor)
                .build();
    }
}
