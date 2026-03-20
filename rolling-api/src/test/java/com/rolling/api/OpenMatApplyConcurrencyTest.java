package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.openmat.repository.OpenMatRepository;
import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:b11-openmat;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "jwt.secret=MDEyMzQ1Njc4OTAxMjM0NTY3ODkwMTIzNDU2Nzg5MDE=",
        "jwt.access-token-expiry=1800000",
        "jwt.refresh-token-expiry=1209600000",
        "spring.profiles.active=prod",
        "firebase.enabled=false",
        "openmat.status.schedule.enabled=false",
        "tournament.crawler.schedule.enabled=false",
        "cloud.aws.s3.bucket=test-bucket",
        "cloud.aws.credentials.access-key=test-access-key",
        "cloud.aws.credentials.secret-key=test-secret-key",
        "cloud.aws.region.static=ap-northeast-2"
})
class OpenMatApplyConcurrencyTest {

    @Autowired
    private OpenMatService openMatService;

    @Autowired
    private OpenMatRepository openMatRepository;

    @Autowired
    private UserRepository userRepository;

    @AfterEach
    void tearDown() {
        openMatRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("정원 1명 경계에서 동시에 신청하면 한 명만 성공한다")
    void apply_concurrently_onlyOneSucceedsAtCapacityBoundary() throws Exception {
        User host = saveUser("host-concurrency", "host");
        User applicantOne = saveUser("applicant-one", "user1");
        User applicantTwo = saveUser("applicant-two", "user2");

        OpenMat openMat = openMatRepository.saveAndFlush(OpenMat.builder()
                .host(host)
                .title("동시성 테스트 오픈매트")
                .description("정원 경계 검증")
                .startDateTime(LocalDateTime.now().plusDays(1))
                .endDateTime(LocalDateTime.now().plusDays(1).plusHours(2))
                .locationName("Rolling Gym")
                .address("Seoul")
                .region(Region.SEOUL)
                .maxCapacity(1)
                .status(OpenMatStatus.RECRUITING)
                .build());

        ExecutorService executorService = Executors.newFixedThreadPool(2);
        CountDownLatch readyLatch = new CountDownLatch(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<String> results = new CopyOnWriteArrayList<>();

        List<Future<?>> futures = List.of(
                executorService.submit(() -> applyAndRecord(applicantOne.getId(), openMat.getId(), readyLatch, startLatch, results)),
                executorService.submit(() -> applyAndRecord(applicantTwo.getId(), openMat.getId(), readyLatch, startLatch, results))
        );

        assertThat(readyLatch.await(5, TimeUnit.SECONDS)).isTrue();
        startLatch.countDown();

        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }
        executorService.shutdown();
        assertThat(executorService.awaitTermination(5, TimeUnit.SECONDS)).isTrue();

        OpenMatResponse response = openMatService.findById(openMat.getId());
        List<String> successResults = results.stream()
                .filter(result -> result.startsWith("SUCCESS"))
                .toList();
        List<String> failureCodes = results.stream()
                .filter(result -> !result.startsWith("SUCCESS"))
                .toList();

        assertThat(successResults).hasSize(1);
        assertThat(failureCodes).hasSize(1);
        assertThat(failureCodes.get(0)).isIn("OPEN_MAT_CLOSED", "CAPACITY_FULL");
        assertThat(response.getCurrentParticipants()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo(OpenMatStatus.CLOSED);
    }

    private void applyAndRecord(
            Long userId,
            Long openMatId,
            CountDownLatch readyLatch,
            CountDownLatch startLatch,
            List<String> results
    ) {
        readyLatch.countDown();
        try {
            assertThat(startLatch.await(5, TimeUnit.SECONDS)).isTrue();
            openMatService.apply(userId, openMatId);
            results.add("SUCCESS:" + userId);
        } catch (BusinessException exception) {
            results.add(exception.getCode());
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    private User saveUser(String socialId, String nickname) {
        return userRepository.save(User.builder()
                .socialId(socialId)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname(nickname)
                .email(socialId + "@rolling.test")
                .beltColor(BeltColor.WHITE)
                .build());
    }
}
