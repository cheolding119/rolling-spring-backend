package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingCard;
import com.rolling.api.domain.traininglog.entity.TrainingCardLevel;
import com.rolling.api.domain.traininglog.entity.TrainingCardPosition;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:training-card-repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
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
class TrainingCardRepositoryTest {

    @Autowired
    private TrainingCardRepository trainingCardRepository;

    @AfterEach
    void tearDown() {
        trainingCardRepository.deleteAll();
    }

    @Test
    @DisplayName("검색어 없이도 레벨과 포지션 필터만으로 활성 카드 목록을 조회한다")
    void findAllActiveCards_returnsCardsWithoutSearchQuery() {
        trainingCardRepository.save(trainingCard(
                "Knee Cut Pass",
                TrainingCardLevel.BEGINNER,
                TrainingCardPosition.GUARD,
                true,
                0
        ));
        trainingCardRepository.save(trainingCard(
                "Body Lock Pass",
                TrainingCardLevel.BEGINNER,
                TrainingCardPosition.STANDING,
                true,
                1
        ));
        trainingCardRepository.save(trainingCard(
                "Inactive Card",
                TrainingCardLevel.BEGINNER,
                TrainingCardPosition.GUARD,
                false,
                2
        ));

        List<TrainingCard> result = trainingCardRepository.findAllActiveCards(
                TrainingCardLevel.BEGINNER,
                TrainingCardPosition.GUARD
        );

        assertThat(result).extracting(TrainingCard::getTitle)
                .containsExactly("Knee Cut Pass");
    }

    @Test
    @DisplayName("검색어가 있으면 제목 요약 분류를 기준으로 부분 일치 검색한다")
    void searchActiveCards_filtersByPattern() {
        trainingCardRepository.save(trainingCard(
                "Knee Cut Pass",
                TrainingCardLevel.BEGINNER,
                TrainingCardPosition.GUARD,
                true,
                0
        ));
        trainingCardRepository.save(trainingCard(
                "Scissor Sweep",
                TrainingCardLevel.BEGINNER,
                TrainingCardPosition.GUARD,
                true,
                1
        ));

        List<TrainingCard> result = trainingCardRepository.searchActiveCards(
                "%knee%",
                TrainingCardLevel.BEGINNER,
                TrainingCardPosition.GUARD
        );

        assertThat(result).extracting(TrainingCard::getTitle)
                .containsExactly("Knee Cut Pass");
    }

    private TrainingCard trainingCard(
            String title,
            TrainingCardLevel level,
            TrainingCardPosition position,
            boolean active,
            int displayOrder
    ) {
        return TrainingCard.builder()
                .title(title)
                .summary(title + " summary")
                .topic("PASS")
                .level(level)
                .position(position)
                .situationSummary(title + " situation")
                .description(title + " description")
                .situationDescription(title + " situation description")
                .startingPositionDescription(title + " start")
                .flowDescription(title + " flow")
                .keyPoints(title + " key points")
                .commonMistakes(title + " mistakes")
                .cautions(title + " cautions")
                .youtubeUrl("https://www.youtube.com/watch?v=test")
                .active(active)
                .displayOrder(displayOrder)
                .build();
    }
}
