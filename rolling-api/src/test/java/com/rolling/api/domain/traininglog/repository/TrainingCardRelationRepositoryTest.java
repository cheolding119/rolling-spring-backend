package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.traininglog.entity.TrainingCard;
import com.rolling.api.domain.traininglog.entity.TrainingCardLevel;
import com.rolling.api.domain.traininglog.entity.TrainingCardPosition;
import com.rolling.api.domain.traininglog.entity.TrainingCardRelation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:training-card-relation-repo;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
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
class TrainingCardRelationRepositoryTest {

    @Autowired
    private TrainingCardRepository trainingCardRepository;

    @Autowired
    private TrainingCardRelationRepository trainingCardRelationRepository;

    @AfterEach
    void tearDown() {
        trainingCardRelationRepository.deleteAll();
        trainingCardRepository.deleteAll();
    }

    @Test
    @DisplayName("연관 카드 조회는 활성 카드만 노출하고 displayOrder 순서를 유지한다")
    void findActiveRelationsByCardId_returnsOnlyActiveRelatedCardsInOrder() {
        TrainingCard sourceCard = trainingCardRepository.save(trainingCard(
                "Knee Cut Pass",
                TrainingCardLevel.BEGINNER,
                TrainingCardPosition.GUARD,
                true,
                0
        ));
        TrainingCard secondCard = trainingCardRepository.save(trainingCard(
                "Toreando Pass",
                TrainingCardLevel.INTERMEDIATE,
                TrainingCardPosition.STANDING,
                true,
                1
        ));
        TrainingCard firstCard = trainingCardRepository.save(trainingCard(
                "Leg Drag Pass",
                TrainingCardLevel.ADVANCED,
                TrainingCardPosition.GUARD,
                true,
                2
        ));
        TrainingCard inactiveCard = trainingCardRepository.save(trainingCard(
                "Inactive Pass",
                TrainingCardLevel.BEGINNER,
                TrainingCardPosition.GUARD,
                false,
                3
        ));

        trainingCardRelationRepository.save(TrainingCardRelation.builder()
                .card(sourceCard)
                .relatedCard(secondCard)
                .displayOrder(1)
                .build());
        trainingCardRelationRepository.save(TrainingCardRelation.builder()
                .card(sourceCard)
                .relatedCard(firstCard)
                .displayOrder(0)
                .build());
        trainingCardRelationRepository.save(TrainingCardRelation.builder()
                .card(sourceCard)
                .relatedCard(inactiveCard)
                .displayOrder(2)
                .build());

        List<TrainingCardRelation> result = trainingCardRelationRepository.findActiveRelationsByCardId(sourceCard.getId());

        assertThat(result).extracting(relation -> relation.getRelatedCard().getTitle())
                .containsExactly("Leg Drag Pass", "Toreando Pass");
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
