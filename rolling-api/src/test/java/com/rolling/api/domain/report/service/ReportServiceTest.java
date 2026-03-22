package com.rolling.api.domain.report.service;

import com.rolling.api.domain.report.dto.ReportResponse;
import com.rolling.api.domain.report.dto.ReportStatusUpdateRequest;
import com.rolling.api.domain.report.entity.Report;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.report.entity.ReportTargetType;
import com.rolling.api.domain.report.repository.ReportRepository;
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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-03-20T03:00:00Z"), SEOUL_ZONE);
        reportService = new ReportService(reportRepository, userRepository, fixedClock);
    }

    @Test
    @DisplayName("신고 생성 시 RECEIVED 상태로 저장한다")
    void createReport_savesReport() {
        User reporter = createUser(1L, "reporter", "reporter");
        when(userRepository.findByIdAndIsWithdrawnFalse(1L)).thenReturn(Optional.of(reporter));
        when(reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(1L, ReportTargetType.OPEN_MAT, 10L))
                .thenReturn(false);
        when(reportRepository.saveAndFlush(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 99L);
            ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.of(2026, 3, 20, 12, 0));
            ReflectionTestUtils.setField(report, "updatedAt", LocalDateTime.of(2026, 3, 20, 12, 0));
            return report;
        });

        Report report = reportService.createReport(
                1L,
                ReportTargetType.OPEN_MAT,
                10L,
                2L,
                ReportReason.OTHER,
                "  부적절한 홍보 글입니다  "
        );

        ArgumentCaptor<Report> captor = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReportStatus.RECEIVED);
        assertThat(report.getId()).isEqualTo(99L);
        assertThat(report.getReporter().getId()).isEqualTo(1L);
        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.OPEN_MAT);
        assertThat(report.getTargetId()).isEqualTo(10L);
        assertThat(report.getReason()).isEqualTo(ReportReason.OTHER);
        assertThat(report.getCustomReason()).isEqualTo("부적절한 홍보 글입니다");
    }

    @Test
    @DisplayName("관리자 신고 목록 조회는 createdAt 내림차순 기본 정렬과 누적 현황을 적용한다")
    void findAllForAdmin_appliesDefaultSortAndIncludesSummary() {
        Report report = createReportEntity(99L, createUser(1L, "reporter", "rolling-user"), ReportStatus.RECEIVED);
        when(reportRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(report)));
        mockSummaryCounts(ReportTargetType.OPEN_MAT, 10L, 4, 1, 1, 2, 0);

        Page<ReportResponse> response = reportService.findAllForAdmin(
                ReportStatus.RECEIVED,
                ReportTargetType.OPEN_MAT,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2026, 3, 31),
                PageRequest.of(0, 200, org.springframework.data.domain.Sort.by("createdAt").ascending())
        );

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(reportRepository).findAll(any(Specification.class), captor.capture());
        assertThat(captor.getValue().getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(captor.getValue().getSort().getOrderFor("createdAt").isAscending()).isTrue();
        assertThat(captor.getValue().getPageSize()).isEqualTo(100);
        assertThat(response.getContent()).hasSize(1);
        assertThat(response.getContent().get(0).getTargetSummary().getTotalReportCount()).isEqualTo(4);
        assertThat(response.getContent().get(0).getTargetSummary().getResolvedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("관리자 신고 상태 변경 시 처리자, 처리 시각, 메모, 최종 조치를 기록한다")
    void updateStatus_recordsProcessingMetadata() {
        Report report = createReportEntity(77L, createUser(2L, "reporter", "rolling-user"), ReportStatus.RECEIVED);
        ReportStatusUpdateRequest request = new ReportStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", ReportStatus.RESOLVED);
        ReflectionTestUtils.setField(request, "processingMemo", "  반복 광고성 내용으로 확인되어 숨김 처리했습니다. ");
        ReflectionTestUtils.setField(request, "finalAction", "  CONTENT_HIDDEN  ");

        when(reportRepository.findVisibleByIdForAdmin(77L, 3L)).thenReturn(Optional.of(report));
        mockSummaryCounts(ReportTargetType.OPEN_MAT, 10L, 4, 0, 1, 3, 0);

        ReportResponse response = reportService.updateStatus(1L, 77L, request);

        assertThat(response.getStatus()).isEqualTo(ReportStatus.RESOLVED);
        assertThat(response.getProcessedByUserId()).isEqualTo(1L);
        assertThat(response.getProcessedAt()).isEqualTo(LocalDateTime.of(2026, 3, 20, 12, 0));
        assertThat(response.getProcessingMemo()).isEqualTo("반복 광고성 내용으로 확인되어 숨김 처리했습니다.");
        assertThat(response.getFinalAction()).isEqualTo("CONTENT_HIDDEN");
        assertThat(response.getTargetSummary().getResolvedCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("관리자 신고 상세 조회는 누적 신고 3건 미만 대상을 숨긴다")
    void findByIdForAdmin_hidesTargetBelowThreshold() {
        when(reportRepository.findVisibleByIdForAdmin(88L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.findByIdForAdmin(88L))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("NOT_FOUND");
                    assertThat(exception).hasMessage("신고를 찾을 수 없습니다");
                });
    }

    @Test
    @DisplayName("관리자 신고 상태 변경도 누적 신고 3건 미만 대상은 허용하지 않는다")
    void updateStatus_rejectsTargetBelowThreshold() {
        ReportStatusUpdateRequest request = new ReportStatusUpdateRequest();
        ReflectionTestUtils.setField(request, "status", ReportStatus.IN_REVIEW);
        when(reportRepository.findVisibleByIdForAdmin(89L, 3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reportService.updateStatus(1L, 89L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo("NOT_FOUND");
                    assertThat(exception).hasMessage("신고를 찾을 수 없습니다");
                });
    }

    @Test
    @DisplayName("이미 신고한 대상은 다시 신고할 수 없다")
    void createReport_rejectsDuplicateReport() {
        User reporter = createUser(1L, "reporter", "reporter");
        when(userRepository.findByIdAndIsWithdrawnFalse(1L)).thenReturn(Optional.of(reporter));
        when(reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(1L, ReportTargetType.OPEN_MAT, 10L))
                .thenReturn(true);

        assertThatThrownBy(() -> reportService.createReport(
                1L,
                ReportTargetType.OPEN_MAT,
                10L,
                2L,
                ReportReason.SPAM,
                null
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo("ALREADY_REPORTED");
            assertThat(exception).hasMessage("이미 신고한 대상입니다");
        });
    }

    @Test
    @DisplayName("자신이 작성한 게시글은 신고할 수 없다")
    void createReport_rejectsSelfReport() {
        assertThatThrownBy(() -> reportService.createReport(
                1L,
                ReportTargetType.OPEN_MAT,
                10L,
                1L,
                ReportReason.FALSE_INFO,
                null
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo("SELF_REPORT_NOT_ALLOWED");
            assertThat(exception).hasMessage("자신이 작성한 게시글은 신고할 수 없습니다");
        });
    }

    @Test
    @DisplayName("기타 신고 사유는 customReason이 필수다")
    void createReport_requiresCustomReasonForOther() {
        User reporter = createUser(1L, "reporter", "reporter");
        when(userRepository.findByIdAndIsWithdrawnFalse(1L)).thenReturn(Optional.of(reporter));
        when(reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(1L, ReportTargetType.OPEN_MAT, 10L))
                .thenReturn(false);

        assertThatThrownBy(() -> reportService.createReport(
                1L,
                ReportTargetType.OPEN_MAT,
                10L,
                2L,
                ReportReason.OTHER,
                "   "
        )).isInstanceOfSatisfying(BusinessException.class, exception -> {
            assertThat(exception.getCode()).isEqualTo("VALIDATION_ERROR");
            assertThat(exception).hasMessage("기타 신고 사유를 입력해주세요");
        });
    }

    @Test
    @DisplayName("기타 외 신고 사유에서는 customReason을 저장하지 않는다")
    void createReport_ignoresCustomReasonWhenReasonIsNotOther() {
        User reporter = createUser(1L, "reporter", "reporter");
        when(userRepository.findByIdAndIsWithdrawnFalse(1L)).thenReturn(Optional.of(reporter));
        when(reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(1L, ReportTargetType.OPEN_MAT, 10L))
                .thenReturn(false);
        when(reportRepository.saveAndFlush(any(Report.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Report report = reportService.createReport(
                1L,
                ReportTargetType.OPEN_MAT,
                10L,
                2L,
                ReportReason.SPAM,
                "무시될 값"
        );

        assertThat(report.getCustomReason()).isNull();
        verify(reportRepository).saveAndFlush(any(Report.class));
    }

    private void mockSummaryCounts(ReportTargetType targetType,
                                   Long targetId,
                                   long total,
                                   long received,
                                   long inReview,
                                   long resolved,
                                   long rejected) {
        when(reportRepository.countByTargetTypeAndTargetId(targetType, targetId)).thenReturn(total);
        when(reportRepository.countByTargetTypeAndTargetIdAndStatus(targetType, targetId, ReportStatus.RECEIVED)).thenReturn(received);
        when(reportRepository.countByTargetTypeAndTargetIdAndStatus(targetType, targetId, ReportStatus.IN_REVIEW)).thenReturn(inReview);
        when(reportRepository.countByTargetTypeAndTargetIdAndStatus(targetType, targetId, ReportStatus.RESOLVED)).thenReturn(resolved);
        when(reportRepository.countByTargetTypeAndTargetIdAndStatus(targetType, targetId, ReportStatus.REJECTED)).thenReturn(rejected);
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

    private Report createReportEntity(Long id, User reporter, ReportStatus status) {
        Report report = Report.builder()
                .reporter(reporter)
                .targetType(ReportTargetType.OPEN_MAT)
                .targetId(10L)
                .reason(ReportReason.SPAM)
                .customReason(null)
                .status(status)
                .build();
        ReflectionTestUtils.setField(report, "id", id);
        ReflectionTestUtils.setField(report, "createdAt", LocalDateTime.of(2026, 3, 20, 9, 0));
        ReflectionTestUtils.setField(report, "updatedAt", LocalDateTime.of(2026, 3, 20, 9, 0));
        return report;
    }
}
