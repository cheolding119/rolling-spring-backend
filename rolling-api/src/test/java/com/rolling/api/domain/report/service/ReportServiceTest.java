package com.rolling.api.domain.report.service;

import com.rolling.api.domain.report.entity.Report;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportTargetType;
import com.rolling.api.domain.report.repository.ReportRepository;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.SocialProvider;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ReportService reportService;

    @Test
    @DisplayName("신고 생성 시 중복이 아니면 신고를 저장한다")
    void createReport_savesReport() {
        User reporter = createUser(1L, "reporter");
        when(userRepository.findByIdAndIsWithdrawnFalse(1L)).thenReturn(Optional.of(reporter));
        when(reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(1L, ReportTargetType.OPEN_MAT, 10L))
                .thenReturn(false);
        when(reportRepository.saveAndFlush(any(Report.class))).thenAnswer(invocation -> {
            Report report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 99L);
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

        assertThat(report.getId()).isEqualTo(99L);
        assertThat(report.getReporter().getId()).isEqualTo(1L);
        assertThat(report.getTargetType()).isEqualTo(ReportTargetType.OPEN_MAT);
        assertThat(report.getTargetId()).isEqualTo(10L);
        assertThat(report.getReason()).isEqualTo(ReportReason.OTHER);
        assertThat(report.getCustomReason()).isEqualTo("부적절한 홍보 글입니다");
    }

    @Test
    @DisplayName("이미 신고한 대상은 다시 신고할 수 없다")
    void createReport_rejectsDuplicateReport() {
        User reporter = createUser(1L, "reporter");
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
        User reporter = createUser(1L, "reporter");
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
        User reporter = createUser(1L, "reporter");
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

    private User createUser(Long id, String socialId) {
        User user = User.builder()
                .socialId(socialId)
                .socialProvider(SocialProvider.GOOGLE)
                .nickname("user-" + id)
                .email(socialId + "@test.com")
                .beltColor(BeltColor.WHITE)
                .build();
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
