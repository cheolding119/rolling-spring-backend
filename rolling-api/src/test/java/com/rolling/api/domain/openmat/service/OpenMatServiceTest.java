package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.entity.OpenMat;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.openmat.repository.OpenMatRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenMatServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private OpenMatRepository openMatRepository;

    @Mock
    private UserRepository userRepository;

    private OpenMatService openMatService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), SEOUL_ZONE);
        openMatService = new OpenMatService(openMatRepository, userRepository, fixedClock);
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