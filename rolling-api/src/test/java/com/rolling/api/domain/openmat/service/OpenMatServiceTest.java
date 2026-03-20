package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.dto.OpenMatUpdateRequest;
import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.notification.repository.NotificationRepository;
import com.rolling.api.domain.openmat.event.OpenMatDeletedEvent;
import com.rolling.api.domain.openmat.event.OpenMatUpdatedEvent;
import com.rolling.api.domain.openmat.repository.OpenMatRepository;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportTargetType;
import com.rolling.api.domain.report.service.ReportService;
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
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenMatServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private OpenMatRepository openMatRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ReportService reportService;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private OpenMatService openMatService;
    private Clock fixedClock;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), SEOUL_ZONE);
        openMatService = new OpenMatService(
                openMatRepository,
                notificationRepository,
                userRepository,
                reportService,
                fixedClock,
                applicationEventPublisher);
    }

    @Test
    @DisplayName("오픈매트 신청으로 정원이 꽉 차면 상태가 CLOSED로 전환된다")
    void apply_whenCapacityReached_setsClosed() {
        User host = createUser(1L, "host-1", "host");
        OpenMat openMat = createOpenMat(
                10L,
                host,
                LocalDateTime.of(2026, 3, 11, 19, 0),
                LocalDateTime.of(2026, 3, 11, 21, 0),
                2,
                OpenMatStatus.RECRUITING,
                2L
        );

        when(openMatRepository.findByIdForUpdate(10L)).thenReturn(java.util.Optional.of(openMat));

        openMatService.apply(3L, 10L);

        assertThat(openMat.getParticipantUids()).containsExactly(2L, 3L);
        assertThat(openMat.getStatus()).isEqualTo(OpenMatStatus.CLOSED);
    }

    @Test
    @DisplayName("오픈매트 신청 취소로 여유가 생기면 상태가 RECRUITING으로 복귀한다")
    void cancelApply_whenCapacityAvailable_reopensRecruiting() {
        User host = createUser(1L, "host-2", "host");
        OpenMat openMat = createOpenMat(
                11L,
                host,
                LocalDateTime.of(2026, 3, 11, 19, 0),
                LocalDateTime.of(2026, 3, 11, 21, 0),
                2,
                OpenMatStatus.CLOSED,
                2L, 3L
        );

        when(openMatRepository.findByIdForUpdate(11L)).thenReturn(java.util.Optional.of(openMat));

        openMatService.cancelApply(3L, 11L);

        assertThat(openMat.getParticipantUids()).containsExactly(2L);
        assertThat(openMat.getStatus()).isEqualTo(OpenMatStatus.RECRUITING);
    }

    @Test
    @DisplayName("신고가 3건 이상인 오픈매트는 신규 신청을 차단한다")
    void apply_whenReportedOpenMat_throwsBusinessException() {
        User host = createUser(1L, "host-3", "host");
        OpenMat openMat = createOpenMat(
                12L,
                host,
                LocalDateTime.of(2026, 3, 11, 19, 0),
                LocalDateTime.of(2026, 3, 11, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        openMat.report();
        openMat.report();
        openMat.report();

        when(openMatRepository.findByIdForUpdate(12L)).thenReturn(java.util.Optional.of(openMat));

        assertThatThrownBy(() -> openMatService.apply(4L, 12L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("OPEN_MAT_REPORTED");
                    assertThat(exception).hasMessage("신고 누적으로 신청이 차단된 오픈매트입니다");
                });
    }

    @Test
    @DisplayName("오픈매트 상세 조회 응답에는 호스트 닉네임이 포함된다")
    void findById_returnsHostNickname() {
        User host = createUser(1L, "host-detail", "rolling-host");
        OpenMat openMat = createOpenMat(
                31L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING,
                2L
        );

        when(openMatRepository.findByIdAndIsHiddenFalse(31L)).thenReturn(java.util.Optional.of(openMat));

        OpenMatResponse response = openMatService.findById(31L);

        assertThat(response.getHostId()).isEqualTo(1L);
        assertThat(response.getHostNickname()).isEqualTo("rolling-host");
    }
    @Test
    @DisplayName("상세 조회 시 종료 시간이 지난 오픈매트는 FINISHED로 보정된다")
    void findById_whenExpired_returnsFinishedStatus() {
        User host = createUser(1L, "host-4", "host");
        OpenMat openMat = createOpenMat(
                13L,
                host,
                LocalDateTime.of(2026, 3, 9, 19, 0),
                LocalDateTime.of(2026, 3, 9, 21, 0),
                20,
                OpenMatStatus.RECRUITING,
                2L
        );

        when(openMatRepository.findByIdAndIsHiddenFalse(13L)).thenReturn(java.util.Optional.of(openMat));

        OpenMatResponse response = openMatService.findById(13L);

        assertThat(response.getStatus()).isEqualTo(OpenMatStatus.FINISHED);
        assertThat(openMat.getStatus()).isEqualTo(OpenMatStatus.FINISHED);
    }

    @Test
    @DisplayName("삭제된 오픈매트는 삭제 알림 수신자에게 상세 정보를 반환한다")
    void findById_whenDeletedAndNotificationOwner_returnsDeletedResponse() {
        User host = createUser(1L, "host-deleted", "host");
        OpenMat openMat = createOpenMat(
                33L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        LocalDateTime deletedAt = LocalDateTime.of(2026, 3, 10, 21, 0);
        openMat.hide(deletedAt);

        when(openMatRepository.findById(33L)).thenReturn(java.util.Optional.of(openMat));
        when(notificationRepository.existsByUser_IdAndTypeAndTargetId(
                7L,
                com.rolling.api.domain.notification.model.PushNotificationType.OPEN_MAT_DELETED,
                33L
        )).thenReturn(true);

        OpenMatResponse response = openMatService.findById(33L, 7L, false);

        assertThat(response.getDeleted()).isTrue();
        assertThat(response.getDeletedAt()).isEqualTo(deletedAt);
        assertThat(response.getTitle()).isEqualTo(openMat.getTitle());
    }

    @Test
    @DisplayName("삭제된 오픈매트는 관련 없는 사용자가 상세 조회할 수 없다")
    void findById_whenDeletedAndUnauthorized_throwsNotFound() {
        User host = createUser(1L, "host-deleted-block", "host");
        OpenMat openMat = createOpenMat(
                34L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        openMat.hide(LocalDateTime.of(2026, 3, 10, 21, 0));

        when(openMatRepository.findById(34L)).thenReturn(java.util.Optional.of(openMat));
        when(notificationRepository.existsByUser_IdAndTypeAndTargetId(
                8L,
                com.rolling.api.domain.notification.model.PushNotificationType.OPEN_MAT_DELETED,
                34L
        )).thenReturn(false);

        assertThatThrownBy(() -> openMatService.findById(34L, 8L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("오픈매트를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("오픈매트 목록 조회는 status 필터를 사용하고 기본 정렬을 시작 시간 오름차순으로 맞춘다")
    void findAll_usesStatusFilterAndAscendingSort() {
        User host = createUser(1L, "host-5", "host");
        OpenMat openMat = createOpenMat(
                14L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                2,
                OpenMatStatus.CLOSED,
                2L, 3L
        );

        when(openMatRepository.findAllByIsHiddenFalseAndStatusNotAndEndDateTimeLessThanEqual(
                eq(OpenMatStatus.FINISHED), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(openMatRepository.findByIsHiddenFalseAndRegionAndStatus(
                eq(Region.SEOUL), eq(OpenMatStatus.CLOSED), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(openMat)));

        openMatService.findAll(Region.SEOUL, OpenMatStatus.CLOSED, PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(openMatRepository).findByIsHiddenFalseAndRegionAndStatus(
                eq(Region.SEOUL), eq(OpenMatStatus.CLOSED), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getSort().getOrderFor("startDateTime")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("startDateTime").isAscending()).isTrue();
    }

    @Test
    @DisplayName("오픈매트 목록 검색 시 제목, 장소명, 주소 대상으로 DB 검색을 사용한다")
    void findAll_withKeyword_usesSearchQuery() {
        User host = createUser(1L, "host-search", "host");
        OpenMat openMat = createOpenMat(
                16L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING,
                2L
        );

        when(openMatRepository.findAllByIsHiddenFalseAndStatusNotAndEndDateTimeLessThanEqual(
                eq(OpenMatStatus.FINISHED), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(openMatRepository.searchVisible(
                eq(Region.SEOUL), eq(OpenMatStatus.RECRUITING), eq("강남 오픈매트"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(openMat)));

        Page<OpenMatResponse> page = openMatService.findAll(
                Region.SEOUL,
                OpenMatStatus.RECRUITING,
                "  강남   오픈매트  ",
                PageRequest.of(0, 20)
        );

        assertThat(page.getContent()).hasSize(1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(openMatRepository).searchVisible(
                eq(Region.SEOUL), eq(OpenMatStatus.RECRUITING), eq("강남 오픈매트"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("startDateTime")).isNotNull();
    }

    @Test
    @DisplayName("내가 개최한 오픈매트 목록 조회는 호스트 기준으로 페이징 조회하고 기본 정렬을 시작 시간 오름차순으로 맞춘다")
    void findMyHostedOpenMats_returnsHostedOpenMats() {
        User host = createUser(1L, "host-hosted", "host");
        OpenMat openMat = createOpenMat(
                32L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING,
                2L
        );

        when(openMatRepository.findAllByIsHiddenFalseAndStatusNotAndEndDateTimeLessThanEqual(
                eq(OpenMatStatus.FINISHED), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(openMatRepository.findByHost_IdAndIsHiddenFalse(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(openMat)));

        Page<OpenMatResponse> page = openMatService.findMyHostedOpenMats(1L, PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getId()).isEqualTo(32L);
        assertThat(page.getContent().get(0).getHostId()).isEqualTo(1L);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(openMatRepository).findByHost_IdAndIsHiddenFalse(eq(1L), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("startDateTime")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("startDateTime").isAscending()).isTrue();
    }

    @Test
    @DisplayName("만료된 오픈매트 동기화는 FINISHED 상태로 일괄 반영한다")
    void syncExpiredOpenMats_updatesFinishedStatus() {
        User host = createUser(1L, "host-6", "host");
        OpenMat openMat = createOpenMat(
                15L,
                host,
                LocalDateTime.of(2026, 3, 9, 19, 0),
                LocalDateTime.of(2026, 3, 9, 21, 0),
                10,
                OpenMatStatus.CLOSED,
                2L, 3L
        );

        when(openMatRepository.findAllByIsHiddenFalseAndStatusNotAndEndDateTimeLessThanEqual(
                eq(OpenMatStatus.FINISHED), any(LocalDateTime.class)))
                .thenReturn(List.of(openMat));

        int synchronizedCount = openMatService.syncExpiredOpenMats();

        assertThat(synchronizedCount).isEqualTo(1);
        assertThat(openMat.getStatus()).isEqualTo(OpenMatStatus.FINISHED);
    }

    @Test
    @DisplayName("오픈매트 신고 시 공통 신고 저장 후 신고 횟수를 증가시킨다")
    void report_increasesReportCount() {
        User host = createUser(1L, "host-report", "host");
        OpenMat openMat = createOpenMat(
                17L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );

        when(openMatRepository.findByIdForUpdate(17L)).thenReturn(java.util.Optional.of(openMat));

        openMatService.report(9L, 17L, ReportReason.SPAM, null);

        verify(reportService).createReport(9L, ReportTargetType.OPEN_MAT, 17L, 1L, ReportReason.SPAM, null);
        assertThat(openMat.getReportCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("참가자가 있는 오픈매트의 일정이나 장소가 바뀌면 수정 알림 이벤트를 발행한다")
    void update_whenScheduleOrLocationChanged_publishesUpdatedEvent() {
        User host = createUser(1L, "host-update-notify", "host");
        OpenMat openMat = createOpenMat(
                19L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING,
                2L,
                3L
        );
        OpenMatUpdateRequest request = new OpenMatUpdateRequest();
        ReflectionTestUtils.setField(request, "startDateTime", LocalDateTime.of(2026, 3, 12, 20, 0));

        when(openMatRepository.findByIdAndIsHiddenFalse(19L)).thenReturn(java.util.Optional.of(openMat));

        openMatService.update(1L, 19L, request);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(OpenMatUpdatedEvent.class);

        OpenMatUpdatedEvent event = (OpenMatUpdatedEvent) eventCaptor.getValue();
        assertThat(event.openMatId()).isEqualTo(19L);
        assertThat(event.participantUserIds()).containsExactly(2L, 3L);
    }

    @Test
    @DisplayName("참가자가 있어도 제목만 바뀌면 수정 알림 이벤트를 발행하지 않는다")
    void update_whenOnlyTitleChanged_doesNotPublishUpdatedEvent() {
        User host = createUser(1L, "host-update-title", "host");
        OpenMat openMat = createOpenMat(
                20L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING,
                2L
        );
        OpenMatUpdateRequest request = new OpenMatUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "changed title");

        when(openMatRepository.findByIdAndIsHiddenFalse(20L)).thenReturn(java.util.Optional.of(openMat));

        openMatService.update(1L, 20L, request);

        verify(applicationEventPublisher, never()).publishEvent(any());
    }


    @Test
    @DisplayName("참가자가 있는 오픈매트를 삭제하면 삭제 알림 이벤트를 발행한다")
    void delete_whenParticipantsExist_publishesDeletedEvent() {
        User host = createUser(1L, "host-delete-notify", "host");
        OpenMat openMat = createOpenMat(
                21L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING,
                2L,
                3L
        );

        when(openMatRepository.findByIdAndIsHiddenFalse(21L)).thenReturn(java.util.Optional.of(openMat));

        openMatService.delete(1L, 21L);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(OpenMatDeletedEvent.class);

        OpenMatDeletedEvent event = (OpenMatDeletedEvent) eventCaptor.getValue();
        assertThat(event.openMatId()).isEqualTo(21L);
        assertThat(event.participantUserIds()).containsExactly(2L, 3L);
    }

    @Test
    @DisplayName("오픈매트 상세 응답은 신고 누적 3건 이상이면 reported=true를 반환한다")
    void findById_whenReported_returnsReportedTrue() {
        User host = createUser(1L, "host-reported", "host");
        OpenMat openMat = createOpenMat(
                18L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        openMat.report();
        openMat.report();
        openMat.report();

        when(openMatRepository.findByIdAndIsHiddenFalse(18L)).thenReturn(java.util.Optional.of(openMat));

        OpenMatResponse response = openMatService.findById(18L);

        assertThat(response.getReported()).isTrue();
    }

    private User createUser(Long id, String socialId, String nickname) {
        User user = User.builder()
                .socialId(socialId)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname(nickname)
                .email(socialId + "@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private OpenMat createOpenMat(
            Long id,
            User host,
            LocalDateTime startDateTime,
            LocalDateTime endDateTime,
            int maxCapacity,
            OpenMatStatus status,
            Long... participantIds
    ) {
        OpenMat openMat = OpenMat.builder()
                .host(host)
                .title("test open mat")
                .description("description")
                .startDateTime(startDateTime)
                .endDateTime(endDateTime)
                .locationName("rolling gym")
                .address("seoul")
                .region(Region.SEOUL)
                .maxCapacity(maxCapacity)
                .hostInstagramId("rolling_bjj")
                .status(status)
                .build();
        ReflectionTestUtils.setField(openMat, "id", id);
        for (Long participantId : participantIds) {
            openMat.addParticipant(participantId);
        }
        return openMat;
    }
}







