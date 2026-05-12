package com.rolling.api.domain.seminar.service;

import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.seminar.dto.SeminarApplicationResponse;
import com.rolling.api.domain.seminar.dto.SeminarCancelApplicationRequest;
import com.rolling.api.domain.seminar.dto.SeminarCreateRequest;
import com.rolling.api.domain.seminar.dto.SeminarResponse;
import com.rolling.api.domain.seminar.entity.Seminar;
import com.rolling.api.domain.seminar.entity.SeminarApplication;
import com.rolling.api.domain.seminar.entity.SeminarApplicationStatus;
import com.rolling.api.domain.seminar.entity.SeminarStatus;
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

    private SeminarService seminarService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), SEOUL_ZONE);
        seminarService = new SeminarService(seminarRepository, seminarApplicationRepository, userRepository, fixedClock);
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
    @DisplayName("참석 신청은 APPLIED 상태 신청을 생성한다")
    void apply_createsAppliedApplication() {
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
    @DisplayName("내 참석 신청 취소는 APPLIED 신청을 CANCELED로 전환한다")
    void cancelMyApplication_cancelsAppliedApplication() {
        User host = createUser(1L, "host-seminar-cancel", "host");
        User applicant = createUser(2L, "applicant-seminar-cancel", "applicant");
        Seminar seminar = createSeminar(10L, host, 3);
        SeminarApplication application = SeminarApplication.builder()
                .seminar(seminar)
                .user(applicant)
                .status(SeminarApplicationStatus.APPLIED)
                .appliedAt(LocalDateTime.of(2026, 3, 10, 18, 0))
                .build();
        SeminarCancelApplicationRequest request = new SeminarCancelApplicationRequest();
        ReflectionTestUtils.setField(request, "cancelReason", "일정 변경");

        when(seminarRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(seminar));
        when(seminarApplicationRepository.countBySeminar_IdAndStatus(10L, SeminarApplicationStatus.APPLIED))
                .thenReturn(1L);
        when(seminarApplicationRepository.findBySeminarIdAndUserIdForUpdate(10L, 2L))
                .thenReturn(Optional.of(application));

        SeminarApplicationResponse response = seminarService.cancelMyApplication(2L, 10L, request);

        assertThat(response.getStatus()).isEqualTo(SeminarApplicationStatus.CANCELED);
        assertThat(response.getCancelReason()).isEqualTo("일정 변경");
        assertThat(response.getCanceledAt()).isNotNull();
    }

    @Test
    @DisplayName("세미나 삭제 시 활성 신청은 세미나 취소 상태로 전환한다")
    void delete_cancelsAppliedApplicationsBySeminar() {
        User host = createUser(1L, "host-seminar-delete", "host");
        User applicant = createUser(2L, "applicant-seminar-delete", "applicant");
        Seminar seminar = createSeminar(10L, host, 3);
        SeminarApplication application = SeminarApplication.builder()
                .seminar(seminar)
                .user(applicant)
                .status(SeminarApplicationStatus.APPLIED)
                .appliedAt(LocalDateTime.of(2026, 3, 10, 18, 0))
                .build();

        when(seminarRepository.findByIdAndIsHiddenFalse(10L)).thenReturn(Optional.of(seminar));
        when(seminarApplicationRepository.findAllBySeminar_IdAndStatus(10L, SeminarApplicationStatus.APPLIED))
                .thenReturn(List.of(application));

        seminarService.delete(1L, 10L);

        assertThat(seminar.getIsHidden()).isTrue();
        assertThat(application.getStatus()).isEqualTo(SeminarApplicationStatus.SEMINAR_CANCELED);
        assertThat(application.getCanceledAt()).isNotNull();
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
                .build();
        ReflectionTestUtils.setField(seminar, "id", id);
        return seminar;
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
}
