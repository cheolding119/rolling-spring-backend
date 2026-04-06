package com.rolling.api;

import com.rolling.api.domain.inquiry.dto.InquiryResponse;
import com.rolling.api.domain.inquiry.entity.Inquiry;
import com.rolling.api.domain.inquiry.entity.InquiryStatus;
import com.rolling.api.domain.inquiry.entity.InquiryType;
import com.rolling.api.domain.inquiry.repository.InquiryRepository;
import com.rolling.api.domain.inquiry.service.InquiryService;
import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.openmat.repository.OpenMatRepository;
import com.rolling.api.domain.openmat.service.OpenMatService;
import com.rolling.api.domain.report.dto.ReportResponse;
import com.rolling.api.domain.report.entity.Report;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.report.entity.ReportTargetType;
import com.rolling.api.domain.report.repository.ReportRepository;
import com.rolling.api.domain.report.service.ReportService;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:nplusone;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.generate_statistics=true",
        "spring.flyway.enabled=false",
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
class NPlusOneIntegrationTest {

    @Autowired
    private OpenMatService openMatService;

    @Autowired
    private InquiryService inquiryService;

    @Autowired
    private ReportService reportService;

    @Autowired
    private OpenMatRepository openMatRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @AfterEach
    void tearDown() {
        reportRepository.deleteAll();
        inquiryRepository.deleteAll();
        openMatRepository.deleteAll();
        userRepository.deleteAll();
        entityManager.clear();
        statistics().clear();
    }

    @Test
    @DisplayName("오픈매트 목록 조회는 host와 participant count 때문에 N+1로 증가하지 않는다")
    void openMatList_doesNotTriggerNPlusOne() {
        User hostOne = userRepository.saveAndFlush(createUser("openmat-host-1", "host-one"));
        User hostTwo = userRepository.saveAndFlush(createUser("openmat-host-2", "host-two"));

        openMatRepository.saveAndFlush(createOpenMat(hostOne, "주말 오픈매트 A", LocalDateTime.now().plusDays(2), 2, 101L, 102L));
        openMatRepository.saveAndFlush(createOpenMat(hostTwo, "주말 오픈매트 B", LocalDateTime.now().plusDays(3), 1, 201L));

        clearPersistenceContextAndStatistics();

        Page<OpenMatResponse> page = openMatService.findAll(
                Region.SEOUL,
                OpenMatStatus.RECRUITING,
                PageRequest.of(0, 1)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getHostNickname()).isEqualTo("host-one");
        assertThat(page.getContent().get(0).getCurrentParticipants()).isEqualTo(2);
        assertPreparedStatementsAtMost(5);
        assertNoLazyFetches();
    }

    @Test
    @DisplayName("문의 관리자 목록 조회는 user LAZY 초기화로 N+1이 발생하지 않는다")
    void inquiryAdminList_doesNotTriggerNPlusOne() {
        User userOne = userRepository.saveAndFlush(createUser("inquiry-user-1", "alpha"));
        User userTwo = userRepository.saveAndFlush(createUser("inquiry-user-2", "beta"));

        inquiryRepository.saveAndFlush(createInquiry(userOne, "문의 A", InquiryStatus.RECEIVED));
        inquiryRepository.saveAndFlush(createInquiry(userTwo, "문의 B", InquiryStatus.IN_REVIEW));

        clearPersistenceContextAndStatistics();

        Page<InquiryResponse> page = inquiryService.findAllForAdmin(
                null,
                null,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                PageRequest.of(0, 1)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUserNickname()).isIn("alpha", "beta");
        assertPreparedStatementsAtMost(2);
        assertNoLazyFetches();
    }

    @Test
    @DisplayName("신고 관리자 목록 조회는 reporter와 대상 요약 집계 때문에 N+1이 발생하지 않는다")
    void reportAdminList_doesNotTriggerNPlusOne() {
        User reporterOne = userRepository.saveAndFlush(createUser("reporter-1", "reporter-one"));
        User reporterTwo = userRepository.saveAndFlush(createUser("reporter-2", "reporter-two"));
        User reporterThree = userRepository.saveAndFlush(createUser("reporter-3", "reporter-three"));

        reportRepository.saveAllAndFlush(List.of(
                createReport(reporterOne, 9001L, ReportStatus.RECEIVED),
                createReport(reporterTwo, 9001L, ReportStatus.IN_REVIEW),
                createReport(reporterThree, 9001L, ReportStatus.RESOLVED)
        ));

        clearPersistenceContextAndStatistics();

        Page<ReportResponse> page = reportService.findAllForAdmin(
                null,
                ReportTargetType.OPEN_MAT,
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                PageRequest.of(0, 1)
        );

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getReporterNickname()).isIn("reporter-one", "reporter-two", "reporter-three");
        assertThat(page.getContent().get(0).getTargetSummary().getTotalReportCount()).isEqualTo(3);
        assertThat(page.getContent().get(0).getTargetSummary().getResolvedCount()).isEqualTo(1);
        assertPreparedStatementsAtMost(3);
        assertNoLazyFetches();
    }

    private void clearPersistenceContextAndStatistics() {
        entityManager.clear();
        statistics().clear();
    }

    private void assertPreparedStatementsAtMost(long expectedMax) {
        assertThat(statistics().getPrepareStatementCount()).isLessThanOrEqualTo(expectedMax);
    }

    private void assertNoLazyFetches() {
        assertThat(statistics().getEntityFetchCount()).isZero();
        assertThat(statistics().getCollectionFetchCount()).isZero();
    }

    private Statistics statistics() {
        return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    }

    private User createUser(String socialId, String nickname) {
        return User.builder()
                .socialId(socialId)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname(nickname)
                .email(socialId + "@rolling.test")
                .beltColor(BeltColor.WHITE)
                .build();
    }

    private OpenMat createOpenMat(User host, String title, LocalDateTime startDateTime, int maxCapacity, Long... participantIds) {
        OpenMat openMat = OpenMat.builder()
                .host(host)
                .title(title)
                .description("N+1 테스트")
                .startDateTime(startDateTime)
                .endDateTime(startDateTime.plusHours(2))
                .locationName("Rolling Gym")
                .address("Seoul")
                .region(Region.SEOUL)
                .maxCapacity(maxCapacity)
                .status(OpenMatStatus.RECRUITING)
                .build();
        for (Long participantId : participantIds) {
            openMat.addParticipant(participantId);
        }
        return openMat;
    }

    private Inquiry createInquiry(User user, String title, InquiryStatus status) {
        return Inquiry.builder()
                .user(user)
                .title(title)
                .content(title + " 내용")
                .type(InquiryType.OTHER)
                .status(status)
                .build();
    }

    private Report createReport(User reporter, Long targetId, ReportStatus status) {
        return Report.builder()
                .reporter(reporter)
                .targetType(ReportTargetType.OPEN_MAT)
                .targetId(targetId)
                .reason(ReportReason.SPAM)
                .status(status)
                .build();
    }
}
