package com.rolling.api.domain.openmat.service;

import com.rolling.api.domain.openmat.dto.OpenMatCreateRequest;
import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.dto.OpenMatUpdateRequest;
import com.rolling.api.domain.openmat.dto.OpenMatHostStatusUpdateRequest;
import com.rolling.api.domain.openmat.dto.OpenMatParticipantResponse;
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
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
    private SimpleMeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(Instant.parse("2026-03-10T10:00:00Z"), SEOUL_ZONE);
        meterRegistry = new SimpleMeterRegistry();
        openMatService = new OpenMatService(
                openMatRepository,
                notificationRepository,
                userRepository,
                reportService,
                fixedClock,
                applicationEventPublisher);
        ReflectionTestUtils.setField(openMatService, "meterRegistry", meterRegistry);
        ReflectionTestUtils.setField(openMatService, "publicBaseUrl", "https://cdn.test.com");
    }

    @Test
    @DisplayName("오픈매트 생성 시 선택한 장소 좌표를 저장하고 응답에 포함한다")
    void create_withCoordinates_savesAndReturnsCoordinates() {
        User host = createUser(1L, "host-create-coordinate", "host");
        OpenMatCreateRequest request = createOpenMatCreateRequest();
        ReflectionTestUtils.setField(request, "latitude", new BigDecimal("37.5012345"));
        ReflectionTestUtils.setField(request, "longitude", new BigDecimal("127.0398765"));

        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(host));
        when(openMatRepository.save(any(OpenMat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OpenMatResponse response = openMatService.create(1L, request);

        ArgumentCaptor<OpenMat> openMatCaptor = ArgumentCaptor.forClass(OpenMat.class);
        verify(openMatRepository).save(openMatCaptor.capture());
        assertThat(openMatCaptor.getValue().getLatitude()).isEqualByComparingTo("37.5012345");
        assertThat(openMatCaptor.getValue().getLongitude()).isEqualByComparingTo("127.0398765");
        assertThat(response.getLatitude()).isEqualByComparingTo("37.5012345");
        assertThat(response.getLongitude()).isEqualByComparingTo("127.0398765");
    }

    @Test
    @DisplayName("오픈매트 생성 시 이미지 URL 목록을 저장하고 응답에 포함한다")
    void create_withImageUrls_savesAndReturnsImageUrls() {
        User host = createUser(1L, "host-create-images", "host");
        OpenMatCreateRequest request = createOpenMatCreateRequest();
        ReflectionTestUtils.setField(request, "imageUrls", List.of(
                "https://cdn.test.com/openmats/images/1.jpg",
                "https://cdn.test.com/openmats/images/2.png"
        ));

        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(host));
        when(openMatRepository.save(any(OpenMat.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OpenMatResponse response = openMatService.create(1L, request);

        ArgumentCaptor<OpenMat> openMatCaptor = ArgumentCaptor.forClass(OpenMat.class);
        verify(openMatRepository).save(openMatCaptor.capture());
        assertThat(openMatCaptor.getValue().getImageUrlsJson())
                .isEqualTo("[\"https://cdn.test.com/openmats/images/1.jpg\",\"https://cdn.test.com/openmats/images/2.png\"]");
        assertThat(response.getImageUrls())
                .containsExactly("https://cdn.test.com/openmats/images/1.jpg", "https://cdn.test.com/openmats/images/2.png");
    }

    @Test
    @DisplayName("오픈매트 생성 시 허용되지 않은 외부 이미지 URL은 거부한다")
    void create_withDisallowedImageUrl_throwsValidationError() {
        User host = createUser(1L, "host-create-invalid-image", "host");
        OpenMatCreateRequest request = createOpenMatCreateRequest();
        ReflectionTestUtils.setField(request, "imageUrls", List.of("https://evil.example.com/openmats/images/1.jpg"));

        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(host));

        assertThatThrownBy(() -> openMatService.create(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception).hasMessage("허용된 이미지 URL만 사용할 수 있습니다");
                });
        verify(openMatRepository, never()).save(any(OpenMat.class));
    }

    @Test
    @DisplayName("오픈매트 생성 시 중복 이미지 URL은 거부한다")
    void create_withDuplicateImageUrls_throwsValidationError() {
        User host = createUser(1L, "host-create-duplicate-image", "host");
        OpenMatCreateRequest request = createOpenMatCreateRequest();
        ReflectionTestUtils.setField(request, "imageUrls", List.of(
                "https://cdn.test.com/openmats/images/1.jpg",
                "https://cdn.test.com/openmats/images/1.jpg"
        ));

        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(host));

        assertThatThrownBy(() -> openMatService.create(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception).hasMessage("중복된 imageUrl은 허용되지 않습니다");
                });
        verify(openMatRepository, never()).save(any(OpenMat.class));
    }

    @Test
    @DisplayName("오픈매트 수정 시 장소 좌표를 갱신하고 응답에 포함한다")
    void update_withCoordinates_updatesAndReturnsCoordinates() {
        User host = createUser(1L, "host-update-coordinate", "host");
        OpenMat openMat = createOpenMat(
                42L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        OpenMatUpdateRequest request = new OpenMatUpdateRequest();
        ReflectionTestUtils.setField(request, "locationName", "Rolling BJJ Gangnam");
        ReflectionTestUtils.setField(request, "address", "서울시 강남구 테헤란로 1");
        ReflectionTestUtils.setField(request, "latitude", new BigDecimal("37.5000000"));
        ReflectionTestUtils.setField(request, "longitude", new BigDecimal("127.0300000"));

        when(openMatRepository.findByIdAndIsHiddenFalse(42L)).thenReturn(java.util.Optional.of(openMat));

        OpenMatResponse response = openMatService.update(1L, 42L, request);

        assertThat(openMat.getLatitude()).isEqualByComparingTo("37.5000000");
        assertThat(openMat.getLongitude()).isEqualByComparingTo("127.0300000");
        assertThat(response.getLatitude()).isEqualByComparingTo("37.5000000");
        assertThat(response.getLongitude()).isEqualByComparingTo("127.0300000");
    }

    @Test
    @DisplayName("오픈매트 수정 시 이미지 URL 목록을 교체하고 응답에 포함한다")
    void update_withImageUrls_updatesAndReturnsImageUrls() {
        User host = createUser(1L, "host-update-images", "host");
        OpenMat openMat = createOpenMat(
                47L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        ReflectionTestUtils.setField(openMat, "imageUrlsJson", "[\"https://cdn.test.com/openmats/images/old.jpg\"]");
        OpenMatUpdateRequest request = new OpenMatUpdateRequest();
        request.setImageUrls(List.of(
                "https://cdn.test.com/openmats/images/1.jpg",
                "https://cdn.test.com/openmats/images/2.png"
        ));

        when(openMatRepository.findByIdAndIsHiddenFalse(47L)).thenReturn(java.util.Optional.of(openMat));

        OpenMatResponse response = openMatService.update(1L, 47L, request);

        assertThat(openMat.getImageUrlsJson())
                .isEqualTo("[\"https://cdn.test.com/openmats/images/1.jpg\",\"https://cdn.test.com/openmats/images/2.png\"]");
        assertThat(response.getImageUrls())
                .containsExactly("https://cdn.test.com/openmats/images/1.jpg", "https://cdn.test.com/openmats/images/2.png");
    }

    @Test
    @DisplayName("오픈매트 수정 시 imageUrls 필드가 없으면 기존 이미지를 유지한다")
    void update_whenImageUrlsFieldMissing_keepsExistingImages() {
        User host = createUser(1L, "host-keep-images", "host");
        OpenMat openMat = createOpenMat(
                48L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        ReflectionTestUtils.setField(openMat, "imageUrlsJson", "[\"https://cdn.test.com/openmats/images/existing.jpg\"]");
        OpenMatUpdateRequest request = new OpenMatUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "changed title");

        when(openMatRepository.findByIdAndIsHiddenFalse(48L)).thenReturn(java.util.Optional.of(openMat));

        OpenMatResponse response = openMatService.update(1L, 48L, request);

        assertThat(response.getImageUrls()).containsExactly("https://cdn.test.com/openmats/images/existing.jpg");
        assertThat(openMat.getImageUrlsJson()).isEqualTo("[\"https://cdn.test.com/openmats/images/existing.jpg\"]");
    }

    @Test
    @DisplayName("오픈매트 수정 시 빈 배열을 보내면 기존 이미지를 모두 제거한다")
    void update_whenImageUrlsEmptyArray_clearsImages() {
        User host = createUser(1L, "host-clear-images", "host");
        OpenMat openMat = createOpenMat(
                49L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        ReflectionTestUtils.setField(openMat, "imageUrlsJson", "[\"https://cdn.test.com/openmats/images/existing.jpg\"]");
        OpenMatUpdateRequest request = new OpenMatUpdateRequest();
        request.setImageUrls(List.of());

        when(openMatRepository.findByIdAndIsHiddenFalse(49L)).thenReturn(java.util.Optional.of(openMat));

        OpenMatResponse response = openMatService.update(1L, 49L, request);

        assertThat(openMat.getImageUrlsJson()).isNull();
        assertThat(response.getImageUrls()).isEmpty();
    }

    @Test
    @DisplayName("오픈매트 수정 시 imageUrls에 null을 명시하면 요청을 거부한다")
    void update_whenImageUrlsExplicitNull_throwsValidationError() {
        User host = createUser(1L, "host-null-images", "host");
        OpenMat openMat = createOpenMat(
                51L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        OpenMatUpdateRequest request = new OpenMatUpdateRequest();
        request.setImageUrls(null);

        when(openMatRepository.findByIdAndIsHiddenFalse(51L)).thenReturn(java.util.Optional.of(openMat));

        assertThatThrownBy(() -> openMatService.update(1L, 51L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception).hasMessage("imageUrls는 null일 수 없습니다");
                });
    }

    @Test
    @DisplayName("오픈매트 생성 시 위도 범위가 잘못되면 거부한다")
    void create_whenLatitudeOutOfRange_throwsValidationError() {
        User host = createUser(1L, "host-invalid-latitude", "host");
        OpenMatCreateRequest request = createOpenMatCreateRequest();
        ReflectionTestUtils.setField(request, "latitude", new BigDecimal("90.0000001"));
        ReflectionTestUtils.setField(request, "longitude", new BigDecimal("127.0398765"));

        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(host));

        assertThatThrownBy(() -> openMatService.create(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception).hasMessage("위도는 -90부터 90 사이여야 합니다");
                });
        verify(openMatRepository, never()).save(any(OpenMat.class));
    }

    @Test
    @DisplayName("오픈매트 생성 시 위도와 경도 중 하나만 있으면 거부한다")
    void create_whenOnlyOneCoordinateExists_throwsValidationError() {
        User host = createUser(1L, "host-partial-coordinate", "host");
        OpenMatCreateRequest request = createOpenMatCreateRequest();
        request.setLatitude(new BigDecimal("37.5012345"));

        when(userRepository.findById(1L)).thenReturn(java.util.Optional.of(host));

        assertThatThrownBy(() -> openMatService.create(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception).hasMessage("위도와 경도는 함께 전달해야 합니다");
                });
        verify(openMatRepository, never()).save(any(OpenMat.class));
    }

    @Test
    @DisplayName("오픈매트 수정 시 경도 범위가 잘못되면 거부한다")
    void update_whenLongitudeOutOfRange_throwsValidationError() {
        User host = createUser(1L, "host-invalid-longitude", "host");
        OpenMat openMat = createOpenMat(
                43L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        OpenMatUpdateRequest request = new OpenMatUpdateRequest();
        ReflectionTestUtils.setField(request, "latitude", new BigDecimal("37.5012345"));
        ReflectionTestUtils.setField(request, "longitude", new BigDecimal("-180.0000001"));

        when(openMatRepository.findByIdAndIsHiddenFalse(43L)).thenReturn(java.util.Optional.of(openMat));

        assertThatThrownBy(() -> openMatService.update(1L, 43L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
                    assertThat(exception).hasMessage("경도는 -180부터 180 사이여야 합니다");
                });
    }

    @Test
    @DisplayName("오픈매트 수정 시 좌표 필드가 없으면 기존 좌표를 유지한다")
    void update_whenCoordinateFieldsMissing_keepsExistingCoordinates() {
        User host = createUser(1L, "host-coordinate-keep", "host");
        OpenMat openMat = createOpenMat(
                45L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        openMat.updateCoordinates(new BigDecimal("37.5012345"), new BigDecimal("127.0398765"));
        OpenMatUpdateRequest request = new OpenMatUpdateRequest();
        ReflectionTestUtils.setField(request, "title", "changed title");

        when(openMatRepository.findByIdAndIsHiddenFalse(45L)).thenReturn(java.util.Optional.of(openMat));

        OpenMatResponse response = openMatService.update(1L, 45L, request);

        assertThat(response.getLatitude()).isEqualByComparingTo("37.5012345");
        assertThat(response.getLongitude()).isEqualByComparingTo("127.0398765");
    }

    @Test
    @DisplayName("오픈매트 수정 시 좌표를 둘 다 null로 명시하면 기존 좌표를 제거한다")
    void update_whenCoordinatesExplicitNull_clearsExistingCoordinates() {
        User host = createUser(1L, "host-coordinate-clear", "host");
        OpenMat openMat = createOpenMat(
                46L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        openMat.updateCoordinates(new BigDecimal("37.5012345"), new BigDecimal("127.0398765"));
        OpenMatUpdateRequest request = new OpenMatUpdateRequest();
        request.setLatitude(null);
        request.setLongitude(null);

        when(openMatRepository.findByIdAndIsHiddenFalse(46L)).thenReturn(java.util.Optional.of(openMat));

        OpenMatResponse response = openMatService.update(1L, 46L, request);

        assertThat(response.getLatitude()).isNull();
        assertThat(response.getLongitude()).isNull();
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
        assertThat(meterRegistry.get("rolling_openmat_apply_total")
                .tag("result", "success")
                .counter()
                .count()).isEqualTo(1.0);
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
        assertThat(meterRegistry.get("rolling_openmat_apply_total")
                .tag("result", "open_mat_reported")
                .counter()
                .count()).isEqualTo(1.0);
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
        assertThat(response.getImageUrls()).isEmpty();
    }

    @Test
    @DisplayName("오픈매트 응답은 저장된 이미지 JSON이 깨져 있어도 빈 배열로 fallback 한다")
    void findById_whenImageUrlsJsonMalformed_returnsEmptyImageUrls() {
        User host = createUser(1L, "host-malformed-images", "host");
        OpenMat openMat = createOpenMat(
                52L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        ReflectionTestUtils.setField(openMat, "imageUrlsJson", "not-json");

        when(openMatRepository.findByIdAndIsHiddenFalse(52L)).thenReturn(java.util.Optional.of(openMat));

        OpenMatResponse response = openMatService.findById(52L);

        assertThat(response.getImageUrls()).isEmpty();
    }

    @Test
    @DisplayName("차단한 사용자가 개최한 오픈매트 상세는 찾을 수 없다")
    void findById_whenHostBlocked_throwsNotFound() {
        User blockedHost = createUser(1L, "blocked-host-detail", "blocked-host");
        OpenMat openMat = createOpenMat(
                50L,
                blockedHost,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING,
                2L
        );

        when(openMatRepository.findByIdAndIsHiddenFalse(50L)).thenReturn(java.util.Optional.of(openMat));
        when(userRepository.findBlockedUserIdsByUserId(99L)).thenReturn(List.of(1L));

        assertThatThrownBy(() -> openMatService.findById(50L, 99L, false))
                .isInstanceOf(BusinessException.class)
                .hasMessage("오픈매트를 찾을 수 없습니다");
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
    @DisplayName("오픈매트 목록 조회는 status 필터를 사용하고 기본 정렬을 생성일 내림차순으로 맞춘다")
    void findAll_usesStatusFilterAndDescendingSort() {
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
        when(openMatRepository.countParticipantsByOpenMatIds(List.of(14L)))
                .thenReturn(List.of(participantCount(14L, 2L)));

        openMatService.findAll(Region.SEOUL, OpenMatStatus.CLOSED, PageRequest.of(0, 20));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(openMatRepository).findByIsHiddenFalseAndRegionAndStatus(
                eq(Region.SEOUL), eq(OpenMatStatus.CLOSED), pageableCaptor.capture());

        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(pageable.getSort().getOrderFor("id")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("id").isDescending()).isTrue();
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
                isNull(), eq(Region.SEOUL), eq(OpenMatStatus.RECRUITING), eq("강남 오픈매트"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(openMat)));
        when(openMatRepository.countParticipantsByOpenMatIds(List.of(16L)))
                .thenReturn(List.of(participantCount(16L, 1L)));

        Page<OpenMatResponse> page = openMatService.findAll(
                Region.SEOUL,
                OpenMatStatus.RECRUITING,
                "  강남   오픈매트  ",
                PageRequest.of(0, 20)
        );

        assertThat(page.getContent()).hasSize(1);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(openMatRepository).searchVisible(
                isNull(), eq(Region.SEOUL), eq(OpenMatStatus.RECRUITING), eq("강남 오픈매트"), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id")).isNotNull();
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("id").isDescending()).isTrue();
    }

    @Test
    @DisplayName("차단한 사용자가 개최한 오픈매트는 검색 결과에서도 제외한다")
    void findAll_withKeyword_andBlockedHost_excludesBlockedHosts() {
        User blockedHost = createUser(1L, "blocked-host-search", "blocked-host");
        OpenMat visibleOpenMat = createOpenMat(
                17L,
                blockedHost,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING,
                2L
        );

        when(openMatRepository.findAllByIsHiddenFalseAndStatusNotAndEndDateTimeLessThanEqual(
                eq(OpenMatStatus.FINISHED), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(userRepository.findBlockedUserIdsByUserId(99L)).thenReturn(List.of(1L));
        when(openMatRepository.searchVisibleExcludingBlocked(
                eq(Region.SEOUL), eq(OpenMatStatus.RECRUITING), eq("강남 오픈매트"), eq(List.of(1L)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(visibleOpenMat)));
        when(openMatRepository.countParticipantsByOpenMatIds(List.of(17L)))
                .thenReturn(List.of(participantCount(17L, 1L)));

        Page<OpenMatResponse> page = openMatService.findAll(
                Region.SEOUL,
                OpenMatStatus.RECRUITING,
                "  강남   오픈매트  ",
                PageRequest.of(0, 20),
                99L
        );

        assertThat(page.getContent()).extracting(OpenMatResponse::getId).containsExactly(17L);
        verify(openMatRepository).searchVisibleExcludingBlocked(
                eq(Region.SEOUL), eq(OpenMatStatus.RECRUITING), eq("강남 오픈매트"), eq(List.of(1L)), any(Pageable.class));
    }

    @Test
    @DisplayName("차단한 사용자가 개최한 오픈매트는 목록에서 제외한다")
    void findAll_whenViewerBlockedHost_excludesBlockedHosts() {
        User visibleHost = createUser(2L, "visible-host-list", "visible-host");
        OpenMat visibleOpenMat = createOpenMat(
                51L,
                visibleHost,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING,
                3L
        );

        when(openMatRepository.findAllByIsHiddenFalseAndStatusNotAndEndDateTimeLessThanEqual(
                eq(OpenMatStatus.FINISHED), any(LocalDateTime.class)))
                .thenReturn(List.of());
        when(userRepository.findBlockedUserIdsByUserId(99L)).thenReturn(List.of(1L));
        when(openMatRepository.findByIsHiddenFalseAndStatusAndHost_IdNotIn(
                eq(OpenMatStatus.RECRUITING), eq(List.of(1L)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(visibleOpenMat)));
        when(openMatRepository.countParticipantsByOpenMatIds(List.of(51L)))
                .thenReturn(List.of(participantCount(51L, 1L)));

        Page<OpenMatResponse> page = openMatService.findAll(
                null,
                OpenMatStatus.RECRUITING,
                null,
                PageRequest.of(0, 20),
                99L
        );

        assertThat(page.getContent()).extracting(OpenMatResponse::getId).containsExactly(51L);
        verify(openMatRepository).findByIsHiddenFalseAndStatusAndHost_IdNotIn(
                eq(OpenMatStatus.RECRUITING), eq(List.of(1L)), any(Pageable.class));
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
        when(openMatRepository.countParticipantsByOpenMatIds(List.of(32L)))
                .thenReturn(List.of(participantCount(32L, 1L)));

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
    @DisplayName("작성자는 오픈매트 참가자 목록을 신청 순서대로 조회할 수 있다")
    void findParticipants_byHost_returnsOrderedParticipants() {
        User host = createUser(1L, "host-participants", "host");
        User participant1 = createUser(2L, "participant-1", "alpha");
        ReflectionTestUtils.setField(participant1, "affiliation", "롤링짐 강남");
        ReflectionTestUtils.setField(participant1, "beltColor", BeltColor.BLUE);
        ReflectionTestUtils.setField(participant1, "stripeCount", 1);
        User participant2 = createUser(3L, "participant-2", "beta");
        ReflectionTestUtils.setField(participant2, "affiliation", "롤링짐 잠실");
        ReflectionTestUtils.setField(participant2, "beltColor", BeltColor.PURPLE);
        ReflectionTestUtils.setField(participant2, "stripeCount", 2);

        OpenMat openMat = createOpenMat(
                35L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING,
                3L,
                2L
        );

        when(openMatRepository.findByIdAndIsHiddenFalse(35L)).thenReturn(java.util.Optional.of(openMat));
        when(userRepository.findAllByIdInAndIsWithdrawnFalse(List.of(3L, 2L)))
                .thenReturn(List.of(participant1, participant2));

        List<OpenMatParticipantResponse> response = openMatService.findParticipants(1L, 35L);

        assertThat(response).hasSize(2);
        assertThat(response).extracting(OpenMatParticipantResponse::getUserId).containsExactly(3L, 2L);
        assertThat(response).extracting(OpenMatParticipantResponse::getName).containsExactly("beta", "alpha");
        assertThat(response).extracting(OpenMatParticipantResponse::getAffiliation)
                .containsExactly("롤링짐 잠실", "롤링짐 강남");
        assertThat(response).extracting(OpenMatParticipantResponse::getBeltColor)
                .containsExactly(BeltColor.PURPLE, BeltColor.BLUE);
        assertThat(response).extracting(OpenMatParticipantResponse::getStripeCount)
                .containsExactly(2, 1);
    }

    @Test
    @DisplayName("작성자가 아닌 사용자도 참가자 목록을 조회할 수 있다")
    void findParticipants_nonHostCanViewParticipants() {
        User host = createUser(1L, "host-participants-block", "host");
        User participant = createUser(2L, "participant-visible", "alpha");
        ReflectionTestUtils.setField(participant, "affiliation", "롤링짐 강남");
        ReflectionTestUtils.setField(participant, "beltColor", BeltColor.BLUE);
        ReflectionTestUtils.setField(participant, "stripeCount", 3);
        OpenMat openMat = createOpenMat(
                36L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING,
                2L
        );

        when(openMatRepository.findByIdAndIsHiddenFalse(36L)).thenReturn(java.util.Optional.of(openMat));
        when(userRepository.findAllByIdInAndIsWithdrawnFalse(List.of(2L)))
                .thenReturn(List.of(participant));

        List<OpenMatParticipantResponse> response = openMatService.findParticipants(99L, 36L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getUserId()).isEqualTo(2L);
        assertThat(response.get(0).getName()).isEqualTo("alpha");
        assertThat(response.get(0).getAffiliation()).isEqualTo("롤링짐 강남");
        assertThat(response.get(0).getBeltColor()).isEqualTo(BeltColor.BLUE);
        assertThat(response.get(0).getStripeCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("작성자는 참가자를 강제 취소할 수 있고 정원 여유가 생기면 RECRUITING으로 복귀한다")
    void removeParticipant_byHost_reopensRecruitingWhenCapacityAvailable() {
        User host = createUser(1L, "host-remove-participant", "host");
        OpenMat openMat = createOpenMat(
                37L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                2,
                OpenMatStatus.CLOSED,
                2L,
                3L
        );

        when(openMatRepository.findByIdForUpdate(37L)).thenReturn(java.util.Optional.of(openMat));

        openMatService.removeParticipant(1L, 37L, 3L);

        assertThat(openMat.getParticipantUids()).containsExactly(2L);
        assertThat(openMat.getStatus()).isEqualTo(OpenMatStatus.RECRUITING);
    }

    @Test
    @DisplayName("참가자가 없는 사용자를 강제 취소하면 NOT_FOUND를 반환한다")
    void removeParticipant_whenParticipantMissing_throwsNotFound() {
        User host = createUser(1L, "host-remove-missing", "host");
        OpenMat openMat = createOpenMat(
                38L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING,
                2L
        );

        when(openMatRepository.findByIdForUpdate(38L)).thenReturn(java.util.Optional.of(openMat));

        assertThatThrownBy(() -> openMatService.removeParticipant(1L, 38L, 99L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("PARTICIPANT_NOT_FOUND");
                    assertThat(exception).hasMessage("해당 참가자를 찾을 수 없습니다");
                });
    }

    @Test
    @DisplayName("호스트가 모집을 수동 마감하면 참가 취소 후에도 CLOSED를 유지한다")
    void updateHostingStatus_manualClose_keepsClosedAfterCancelApply() {
        User host = createUser(1L, "host-manual-close", "host");
        OpenMat openMat = createOpenMat(
                39L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                5,
                OpenMatStatus.RECRUITING,
                2L,
                3L
        );
        OpenMatHostStatusUpdateRequest request = new OpenMatHostStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", OpenMatStatus.CLOSED);

        when(openMatRepository.findByIdForUpdate(39L)).thenReturn(java.util.Optional.of(openMat));

        openMatService.updateHostingStatus(1L, 39L, request);
        openMatService.cancelApply(2L, 39L);

        assertThat(openMat.getManualClosed()).isTrue();
        assertThat(openMat.getStatus()).isEqualTo(OpenMatStatus.CLOSED);
        assertThat(openMat.getParticipantUids()).containsExactly(3L);
    }

    @Test
    @DisplayName("호스트는 수동 마감한 모집을 다시 RECRUITING으로 열 수 있다")
    void updateHostingStatus_reopenManualClose_setsRecruiting() {
        User host = createUser(1L, "host-manual-reopen", "host");
        OpenMat openMat = createOpenMat(
                40L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                5,
                OpenMatStatus.RECRUITING,
                2L
        );
        OpenMatHostStatusUpdateRequest closeRequest = new OpenMatHostStatusUpdateRequest();
        ReflectionTestUtils.setField(closeRequest, "status", OpenMatStatus.CLOSED);
        OpenMatHostStatusUpdateRequest reopenRequest = new OpenMatHostStatusUpdateRequest();
        ReflectionTestUtils.setField(reopenRequest, "status", OpenMatStatus.RECRUITING);

        when(openMatRepository.findByIdForUpdate(40L)).thenReturn(java.util.Optional.of(openMat));

        openMatService.updateHostingStatus(1L, 40L, closeRequest);
        OpenMatResponse response = openMatService.updateHostingStatus(1L, 40L, reopenRequest);

        assertThat(openMat.getManualClosed()).isFalse();
        assertThat(response.getStatus()).isEqualTo(OpenMatStatus.RECRUITING);
    }

    @Test
    @DisplayName("정원이 가득 찬 오픈매트는 수동으로 RECRUITING으로 열 수 없다")
    void updateHostingStatus_whenCapacityFull_cannotReopenRecruiting() {
        User host = createUser(1L, "host-full-reopen", "host");
        OpenMat openMat = createOpenMat(
                41L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                2,
                OpenMatStatus.CLOSED,
                2L,
                3L
        );
        OpenMatHostStatusUpdateRequest request = new OpenMatHostStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", OpenMatStatus.RECRUITING);

        when(openMatRepository.findByIdForUpdate(41L)).thenReturn(java.util.Optional.of(openMat));

        assertThatThrownBy(() -> openMatService.updateHostingStatus(1L, 41L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("CAPACITY_FULL");
                    assertThat(exception).hasMessage("정원이 가득 찬 오픈매트는 모집중으로 변경할 수 없습니다");
                });
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
        when(openMatRepository.countParticipantsByOpenMatIds(List.of(15L)))
                .thenReturn(List.of(participantCount(15L, 2L)));

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
        assertThat(meterRegistry.get("rolling_openmat_report_total")
                .tag("result", "success")
                .counter()
                .count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("관리자는 신고 누적으로 차단된 오픈매트의 신고 차단을 해제할 수 있다")
    void clearReportBlock_resetsReportCount() {
        User host = createUser(1L, "host-clear-report", "host");
        OpenMat openMat = createOpenMat(
                22L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING
        );
        openMat.report();
        openMat.report();
        openMat.report();

        when(openMatRepository.findById(22L)).thenReturn(java.util.Optional.of(openMat));

        OpenMatResponse response = openMatService.clearReportBlock(100L, 22L);

        assertThat(openMat.getReportCount()).isEqualTo(0);
        assertThat(openMat.isReported()).isFalse();
        assertThat(response.getReported()).isFalse();
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
    @DisplayName("참가자가 있는 오픈매트의 좌표가 바뀌면 수정 알림 이벤트를 발행한다")
    void update_whenCoordinatesChanged_publishesUpdatedEvent() {
        User host = createUser(1L, "host-update-coordinate-notify", "host");
        OpenMat openMat = createOpenMat(
                44L,
                host,
                LocalDateTime.of(2026, 3, 12, 19, 0),
                LocalDateTime.of(2026, 3, 12, 21, 0),
                10,
                OpenMatStatus.RECRUITING,
                2L,
                3L
        );
        OpenMatUpdateRequest request = new OpenMatUpdateRequest();
        ReflectionTestUtils.setField(request, "latitude", new BigDecimal("37.5012345"));
        ReflectionTestUtils.setField(request, "longitude", new BigDecimal("127.0398765"));

        when(openMatRepository.findByIdAndIsHiddenFalse(44L)).thenReturn(java.util.Optional.of(openMat));

        openMatService.update(1L, 44L, request);

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(OpenMatUpdatedEvent.class);

        OpenMatUpdatedEvent event = (OpenMatUpdatedEvent) eventCaptor.getValue();
        assertThat(event.openMatId()).isEqualTo(44L);
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

    private OpenMatCreateRequest createOpenMatCreateRequest() {
        OpenMatCreateRequest request = new OpenMatCreateRequest();
        ReflectionTestUtils.setField(request, "title", "test open mat");
        ReflectionTestUtils.setField(request, "description", "description");
        ReflectionTestUtils.setField(request, "startDateTime", LocalDateTime.of(2026, 3, 12, 19, 0));
        ReflectionTestUtils.setField(request, "endDateTime", LocalDateTime.of(2026, 3, 12, 21, 0));
        ReflectionTestUtils.setField(request, "locationName", "rolling gym");
        ReflectionTestUtils.setField(request, "address", "seoul");
        ReflectionTestUtils.setField(request, "region", Region.SEOUL);
        ReflectionTestUtils.setField(request, "maxCapacity", 10);
        ReflectionTestUtils.setField(request, "hostInstagramId", "rolling_bjj");
        return request;
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

    private OpenMatRepository.OpenMatParticipantCountView participantCount(Long openMatId, Long participantCount) {
        return new OpenMatRepository.OpenMatParticipantCountView() {
            @Override
            public Long getOpenMatId() {
                return openMatId;
            }

            @Override
            public Long getParticipantCount() {
                return participantCount;
            }
        };
    }
}







