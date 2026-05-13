package com.rolling.api.domain.seminar.service;

import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportTargetType;
import com.rolling.api.domain.report.repository.ReportRepository;
import com.rolling.api.domain.report.service.ReportService;
import com.rolling.api.domain.seminar.dto.SeminarApplicationResponse;
import com.rolling.api.domain.seminar.dto.SeminarCancelApplicationRequest;
import com.rolling.api.domain.seminar.dto.SeminarCreateRequest;
import com.rolling.api.domain.seminar.dto.SeminarHostCancelApplicationRequest;
import com.rolling.api.domain.seminar.dto.SeminarResponse;
import com.rolling.api.domain.seminar.dto.SeminarStatusUpdateRequest;
import com.rolling.api.domain.seminar.entity.Seminar;
import com.rolling.api.domain.seminar.entity.SeminarApplication;
import com.rolling.api.domain.seminar.entity.SeminarApplicationStatus;
import com.rolling.api.domain.seminar.entity.SeminarStatus;
import com.rolling.api.domain.seminar.event.SeminarApplicationCanceledByHostEvent;
import com.rolling.api.domain.seminar.event.SeminarApplicationCanceledEvent;
import com.rolling.api.domain.seminar.event.SeminarAppliedEvent;
import com.rolling.api.domain.seminar.event.SeminarCanceledEvent;
import com.rolling.api.domain.seminar.event.SeminarDeletedEvent;
import com.rolling.api.domain.seminar.repository.SeminarApplicationRepository;
import com.rolling.api.domain.seminar.repository.SeminarRepository;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeminarServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private SeminarRepository seminarRepository;

    @Mock
    private SeminarApplicationRepository seminarApplicationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ReportService reportService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private SeminarService seminarService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), SEOUL_ZONE);
        seminarService = new SeminarService(
                seminarRepository,
                seminarApplicationRepository,
                userRepository,
                reportRepository,
                reportService,
                fixedClock,
                eventPublisher
        );
    }

    @Test
    @DisplayName("세미나 생성 시 장소 좌표를 저장하고 응답에 포함한다")
    void create_withCoordinates_savesAndReturnsCoordinates() {
        User host = createUser(1L, "host-seminar-create", "host");
        SeminarCreateRequest request = createSeminarCreateRequest();
        ReflectionTestUtils.setField(request, "latitude", new BigDecimal("37.5012345"));
        ReflectionTestUtils.setField(request, "longitude", new BigDecimal("127.0398765"));

        when(userRepository.findByIdAndIsWithdrawnFalse(1L)).thenReturn(Optional.of(host));
        when(seminarRepository.save(any(Seminar.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeminarResponse response = seminarService.create(1L, request);

        ArgumentCaptor<Seminar> captor = ArgumentCaptor.forClass(Seminar.class);
        verify(seminarRepository).save(captor.capture());
        assertThat(captor.getValue().getLatitude()).isEqualByComparingTo("37.5012345");
        assertThat(captor.getValue().getLongitude()).isEqualByComparingTo("127.0398765");
        assertThat(response.getLatitude()).isEqualByComparingTo("37.5012345");
        assertThat(response.getLongitude()).isEqualByComparingTo("127.0398765");
        assertThat(response.getStatus()).isEqualTo(SeminarStatus.RECRUITING);
    }

    @Test
    @DisplayName("세미나 생성 시 위도와 경도 중 하나만 있으면 거부한다")
    void create_whenOnlyLatitudePresent_throwsValidationError() {
        User host = createUser(1L, "host-seminar-invalid-coordinate", "host");
        SeminarCreateRequest request = createSeminarCreateRequest();
        ReflectionTestUtils.setField(request, "latitude", new BigDecimal("37.5012345"));

        when(userRepository.findByIdAndIsWithdrawnFalse(1L)).thenReturn(Optional.of(host));

        assertThatThrownBy(() -> seminarService.create(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception).hasMessage("위도와 경도는 함께 전달해야 합니다");
                });
        verify(seminarRepository, never()).save(any(Seminar.class));
    }

    @Test
    @DisplayName("참석 신청은 APPLIED 상태 신청을 생성하고 알림 이벤트를 발행한다")
    void apply_createsAppliedApplicationAndPublishesEvent() {
        User host = createUser(1L, "host-seminar-apply", "host");
        User applicant = createUser(2L, "applicant-seminar-apply", "applicant");
        Seminar seminar = createSeminar(10L, host, 3);

        when(seminarRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(seminar));
        when(userRepository.findByIdAndIsWithdrawnFalse(2L)).thenReturn(Optional.of(applicant));
        when(seminarApplicationRepository.countBySeminar_IdAndStatus(10L, SeminarApplicationStatus.APPLIED))
                .thenReturn(0L);
        when(seminarApplicationRepository.findBySeminarIdAndUserIdForUpdate(10L, 2L)).thenReturn(Optional.empty());
        when(seminarApplicationRepository.save(any(SeminarApplication.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeminarApplicationResponse response = seminarService.apply(2L, 10L);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(SeminarAppliedEvent.class);
        assertThat(response.getSeminarId()).isEqualTo(10L);
        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.getStatus()).isEqualTo(SeminarApplicationStatus.APPLIED);
    }

    @Test
    @DisplayName("정원이 찬 세미나는 참석 신청을 거부한다")
    void apply_whenCapacityFull_throwsCapacityFull() {
        User host = createUser(1L, "host-seminar-full", "host");
        User applicant = createUser(2L, "applicant-seminar-full", "applicant");
        Seminar seminar = createSeminar(10L, host, 1);

        when(seminarRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(seminar));
        when(userRepository.findByIdAndIsWithdrawnFalse(2L)).thenReturn(Optional.of(applicant));
        when(seminarApplicationRepository.countBySeminar_IdAndStatus(10L, SeminarApplicationStatus.APPLIED))
                .thenReturn(1L);

        assertThatThrownBy(() -> seminarService.apply(2L, 10L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("CAPACITY_FULL"));
        verify(seminarApplicationRepository, never()).save(any(SeminarApplication.class));
    }

    @Test
    @DisplayName("내 참석 신청 취소는 CANCELED 상태로 전환하고 알림 이벤트를 발행한다")
    void cancelMyApplication_cancelsAppliedApplication() {
        User host = createUser(1L, "host-seminar-cancel", "host");
        User applicant = createUser(2L, "applicant-seminar-cancel", "applicant");
        Seminar seminar = createSeminar(10L, host, 3);
        SeminarApplication application = createApplication(seminar, applicant, SeminarApplicationStatus.APPLIED);
        SeminarCancelApplicationRequest request = new SeminarCancelApplicationRequest();
        ReflectionTestUtils.setField(request, "cancelReason", "일정 변경");

        when(seminarRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(seminar));
        when(seminarApplicationRepository.countBySeminar_IdAndStatus(10L, SeminarApplicationStatus.APPLIED))
                .thenReturn(1L);
        when(seminarApplicationRepository.findBySeminarIdAndUserIdForUpdate(10L, 2L))
                .thenReturn(Optional.of(application));

        SeminarApplicationResponse response = seminarService.cancelMyApplication(2L, 10L, request);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(SeminarApplicationCanceledEvent.class);
        assertThat(response.getStatus()).isEqualTo(SeminarApplicationStatus.CANCELED);
        assertThat(response.getCancelReason()).isEqualTo("일정 변경");
        assertThat(response.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("내 신청 목록은 reported 개인화 필드를 반영한다")
    void findMyApplications_marksReportedSeminars() {
        User host = createUser(1L, "host-seminar-my-applications", "host");
        User applicant = createUser(2L, "applicant-seminar-my-applications", "applicant");
        Seminar seminar = createSeminar(10L, host, 3);
        SeminarApplication application = createApplication(seminar, applicant, SeminarApplicationStatus.APPLIED);

        when(seminarRepository.findAllByIsHiddenFalseAndStatusNotAndEndDateTimeLessThanEqual(eq(SeminarStatus.FINISHED), any()))
                .thenReturn(List.of());
        when(seminarApplicationRepository.findMine(eq(2L), eq(SeminarApplicationStatus.APPLIED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(application), PageRequest.of(0, 10), 1));
        when(seminarApplicationRepository.countBySeminarIdsAndStatus(List.of(10L), SeminarApplicationStatus.APPLIED))
                .thenReturn(List.of(countView(10L, 1L)));
        when(reportRepository.findTargetIdsByReporter_IdAndTargetTypeAndTargetIdIn(2L, ReportTargetType.SEMINAR, List.of(10L)))
                .thenReturn(List.of(10L));

        Page<SeminarResponse> response = seminarService.findMyApplications(2L, null, PageRequest.of(0, 10));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getReported()).isTrue();
        assertThat(response.getContent().get(0).getMyApplicationStatus()).isEqualTo(SeminarApplicationStatus.APPLIED);
    }

    @Test
    @DisplayName("세미나 목록 검색어는 DB가 안전하게 처리할 수 있도록 패턴으로 정규화된다")
    void findAll_normalizesKeywordToLikePattern() {
        User host = createUser(1L, "host-seminar-search", "host");
        Seminar seminar = createSeminar(10L, host, 3);

        when(seminarRepository.findAllByIsHiddenFalseAndStatusNotAndEndDateTimeLessThanEqual(eq(SeminarStatus.FINISHED), any()))
                .thenReturn(List.of());
        when(seminarRepository.searchVisible(
                eq(-1L),
                isNull(),
                isNull(),
                eq("%가드 패스%"),
                isNull(),
                isNull(),
                any(PageRequest.class)
        )).thenReturn(new PageImpl<>(List.of(seminar), PageRequest.of(0, 10), 1));
        when(seminarApplicationRepository.countBySeminarIdsAndStatus(List.of(10L), SeminarApplicationStatus.APPLIED))
                .thenReturn(List.of(countView(10L, 0L)));

        Page<SeminarResponse> response = seminarService.findAll(
                null,
                null,
                "  가드   패스  ",
                null,
                null,
                PageRequest.of(0, 10),
                null
        );

        assertThat(response.getContent()).hasSize(1);
        verify(seminarRepository).searchVisible(
                eq(-1L),
                isNull(),
                isNull(),
                eq("%가드 패스%"),
                isNull(),
                isNull(),
                any(PageRequest.class)
        );
    }

    @Test
    @DisplayName("신청자 목록은 status 파라미터가 없으면 APPLIED를 기본값으로 사용한다")
    void findApplications_defaultsStatusToApplied() {
        User host = createUser(1L, "host-seminar-applications", "host");
        Seminar seminar = createSeminar(10L, host, 3);
        User applicant = createUser(2L, "applicant-seminar-applications", "applicant");
        SeminarApplication application = createApplication(seminar, applicant, SeminarApplicationStatus.APPLIED);

        when(seminarRepository.findByIdAndIsHiddenFalse(10L)).thenReturn(Optional.of(seminar));
        when(seminarApplicationRepository.findBySeminarId(eq(10L), eq(SeminarApplicationStatus.APPLIED), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(application), PageRequest.of(0, 20), 1));

        Page<SeminarApplicationResponse> response = seminarService.findApplications(1L, 10L, null, PageRequest.of(0, 20));

        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getStatus()).isEqualTo(SeminarApplicationStatus.APPLIED);
    }

    @Test
    @DisplayName("호스트가 아니면 신청자 목록을 조회할 수 없다")
    void findApplications_whenNotHost_throwsForbidden() {
        User host = createUser(1L, "host-seminar-applications-forbidden", "host");
        Seminar seminar = createSeminar(10L, host, 3);

        when(seminarRepository.findByIdAndIsHiddenFalse(10L)).thenReturn(Optional.of(seminar));

        assertThatThrownBy(() -> seminarService.findApplications(2L, 10L, null, PageRequest.of(0, 20)))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("FORBIDDEN");
                    assertThat(exception).hasMessage("호스트만 신청자와 모집 상태를 관리할 수 있습니다");
                });
    }

    @Test
    @DisplayName("호스트 강제 취소는 HOST_CANCELED로 전환하고 알림 이벤트를 발행한다")
    void cancelApplicationByHost_cancelsAppliedApplication() {
        User host = createUser(1L, "host-seminar-host-cancel", "host");
        User applicant = createUser(2L, "applicant-seminar-host-cancel", "applicant");
        Seminar seminar = createSeminar(10L, host, 3);
        SeminarApplication application = createApplication(seminar, applicant, SeminarApplicationStatus.APPLIED);
        ReflectionTestUtils.setField(application, "id", 99L);
        SeminarHostCancelApplicationRequest request = new SeminarHostCancelApplicationRequest();
        ReflectionTestUtils.setField(request, "cancelReason", "준비물 미확인");

        when(seminarRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(seminar));
        when(seminarApplicationRepository.countBySeminar_IdAndStatus(10L, SeminarApplicationStatus.APPLIED)).thenReturn(1L);
        when(seminarApplicationRepository.findByIdAndSeminar_Id(99L, 10L)).thenReturn(Optional.of(application));

        SeminarApplicationResponse response = seminarService.cancelApplicationByHost(1L, 10L, 99L, request);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(SeminarApplicationCanceledByHostEvent.class);
        assertThat(response.getStatus()).isEqualTo(SeminarApplicationStatus.HOST_CANCELED);
        assertThat(response.getCancelReason()).isEqualTo("준비물 미확인");
    }

    @Test
    @DisplayName("모집 상태를 CANCELED로 변경하면 활성 신청을 SEMINAR_CANCELED로 일괄 전환한다")
    void updateStatus_canceled_cancelsAppliedApplications() {
        User host = createUser(1L, "host-seminar-status-cancel", "host");
        User applicant = createUser(2L, "applicant-seminar-status-cancel", "applicant");
        Seminar seminar = createSeminar(10L, host, 3);
        SeminarApplication application = createApplication(seminar, applicant, SeminarApplicationStatus.APPLIED);
        SeminarStatusUpdateRequest request = new SeminarStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", SeminarStatus.CANCELED);
        ReflectionTestUtils.setField(request, "reason", "강사 일정 변경");

        when(seminarRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(seminar));
        when(seminarApplicationRepository.countBySeminar_IdAndStatus(10L, SeminarApplicationStatus.APPLIED)).thenReturn(1L);
        when(seminarApplicationRepository.findAllBySeminar_IdAndStatus(10L, SeminarApplicationStatus.APPLIED))
                .thenReturn(List.of(application));

        SeminarResponse response = seminarService.updateStatus(1L, 10L, request);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(SeminarCanceledEvent.class);
        assertThat(seminar.getStatus()).isEqualTo(SeminarStatus.CANCELED);
        assertThat(application.getStatus()).isEqualTo(SeminarApplicationStatus.SEMINAR_CANCELED);
        assertThat(application.getCancelReason()).isEqualTo("강사 일정 변경");
        assertThat(response.getStatus()).isEqualTo(SeminarStatus.CANCELED);
    }

    @Test
    @DisplayName("세미나 삭제 시 활성 신청은 세미나 취소 상태로 전환하고 삭제 이벤트를 발행한다")
    void delete_cancelsAppliedApplicationsBySeminar() {
        User host = createUser(1L, "host-seminar-delete", "host");
        User applicant = createUser(2L, "applicant-seminar-delete", "applicant");
        Seminar seminar = createSeminar(10L, host, 3);
        SeminarApplication application = createApplication(seminar, applicant, SeminarApplicationStatus.APPLIED);

        when(seminarRepository.findByIdAndIsHiddenFalse(10L)).thenReturn(Optional.of(seminar));
        when(seminarApplicationRepository.findAllBySeminar_IdAndStatus(10L, SeminarApplicationStatus.APPLIED))
                .thenReturn(List.of(application));

        seminarService.delete(1L, 10L);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(SeminarDeletedEvent.class);
        assertThat(seminar.getIsHidden()).isTrue();
        assertThat(application.getStatus()).isEqualTo(SeminarApplicationStatus.SEMINAR_CANCELED);
        assertThat(application.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("세미나 신고는 기존 신고 서비스에 SEMINAR targetType으로 위임하고 신고 수를 증가시킨다")
    void report_delegatesToReportServiceAndIncrementsReportCount() {
        User host = createUser(1L, "host-seminar-report", "host");
        Seminar seminar = createSeminar(10L, host, 3);

        when(seminarRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(seminar));

        seminarService.report(2L, 10L, ReportReason.SPAM, null);

        verify(reportService).createReport(2L, ReportTargetType.SEMINAR, 10L, 1L, ReportReason.SPAM, null);
        assertThat(seminar.getReportCount()).isEqualTo(1);
    }

    private SeminarCreateRequest createSeminarCreateRequest() {
        SeminarCreateRequest request = new SeminarCreateRequest();
        ReflectionTestUtils.setField(request, "title", "가드 패스 세미나");
        ReflectionTestUtils.setField(request, "description", "가드 패스 기본기와 드릴을 다룹니다.");
        ReflectionTestUtils.setField(request, "instructorName", "김코치");
        ReflectionTestUtils.setField(request, "startDateTime", LocalDateTime.of(2026, 3, 20, 14, 0));
        ReflectionTestUtils.setField(request, "endDateTime", LocalDateTime.of(2026, 3, 20, 17, 0));
        ReflectionTestUtils.setField(request, "applicationStartDateTime", LocalDateTime.of(2026, 3, 10, 12, 0));
        ReflectionTestUtils.setField(request, "applicationEndDateTime", LocalDateTime.of(2026, 3, 19, 23, 0));
        ReflectionTestUtils.setField(request, "locationName", "Rolling Gym");
        ReflectionTestUtils.setField(request, "address", "서울시 강남구 테헤란로 1");
        ReflectionTestUtils.setField(request, "region", Region.SEOUL);
        ReflectionTestUtils.setField(request, "maxCapacity", 20);
        ReflectionTestUtils.setField(request, "price", 30000);
        return request;
    }

    private Seminar createSeminar(Long id, User host, int maxCapacity) {
        Seminar seminar = Seminar.builder()
                .host(host)
                .title("가드 패스 세미나")
                .description("가드 패스 기본기")
                .instructorName("김코치")
                .startDateTime(LocalDateTime.of(2026, 3, 20, 14, 0))
                .endDateTime(LocalDateTime.of(2026, 3, 20, 17, 0))
                .applicationStartDateTime(LocalDateTime.of(2026, 3, 10, 12, 0))
                .applicationEndDateTime(LocalDateTime.of(2026, 3, 19, 23, 0))
                .locationName("Rolling Gym")
                .address("서울시 강남구 테헤란로 1")
                .region(Region.SEOUL)
                .maxCapacity(maxCapacity)
                .price(30000)
                .status(SeminarStatus.RECRUITING)
                .manualClosed(false)
                .build();
        ReflectionTestUtils.setField(seminar, "id", id);
        return seminar;
    }

    private SeminarApplication createApplication(Seminar seminar, User user, SeminarApplicationStatus status) {
        SeminarApplication application = SeminarApplication.builder()
                .seminar(seminar)
                .user(user)
                .status(status)
                .appliedAt(LocalDateTime.of(2026, 3, 10, 18, 0))
                .build();
        ReflectionTestUtils.setField(application, "id", 50L);
        return application;
    }

    private User createUser(Long id, String socialId, String nickname) {
        User user = User.builder()
                .socialId(socialId)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname(nickname)
                .email(socialId + "@example.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private SeminarApplicationRepository.SeminarApplicationCountView countView(Long seminarId, Long applicationCount) {
        return new SeminarApplicationRepository.SeminarApplicationCountView() {
            @Override
            public Long getSeminarId() {
                return seminarId;
            }

            @Override
            public Long getApplicationCount() {
                return applicationCount;
            }
        };
    }
}
