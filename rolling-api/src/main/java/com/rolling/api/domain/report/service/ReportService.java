package com.rolling.api.domain.report.service;

import com.rolling.api.domain.report.entity.Report;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportTargetType;
import com.rolling.api.domain.report.repository.ReportRepository;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public Report createReport(Long reporterUserId,
                               ReportTargetType targetType,
                               Long targetId,
                               Long ownerUserId,
                               ReportReason reason,
                               String customReason) {
        if (targetType == null || targetId == null) {
            throw BusinessException.badRequest("신고 대상이 올바르지 않습니다");
        }
        if (ownerUserId != null && ownerUserId.equals(reporterUserId)) {
            throw new BusinessException("SELF_REPORT_NOT_ALLOWED", "자신이 작성한 게시글은 신고할 수 없습니다", HttpStatus.BAD_REQUEST);
        }

        User reporter = userRepository.findByIdAndIsWithdrawnFalse(reporterUserId)
                .orElseThrow(() -> BusinessException.notFound("사용자를 찾을 수 없습니다"));

        if (reportRepository.existsByReporter_IdAndTargetTypeAndTargetId(reporterUserId, targetType, targetId)) {
            throw alreadyReportedException();
        }

        Report report = Report.builder()
                .reporter(reporter)
                .targetType(targetType)
                .targetId(targetId)
                .reason(requireReason(reason))
                .customReason(normalizeCustomReason(reason, customReason))
                .build();

        try {
            return reportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            throw alreadyReportedException();
        }
    }

    private ReportReason requireReason(ReportReason reason) {
        if (reason == null) {
            throw BusinessException.badRequest("신고 사유는 필수입니다");
        }
        return reason;
    }

    private String normalizeCustomReason(ReportReason reason, String customReason) {
        String normalized = customReason == null ? null : customReason.trim();

        if (reason == ReportReason.OTHER) {
            if (normalized == null || normalized.isBlank()) {
                throw BusinessException.badRequest("기타 신고 사유를 입력해주세요");
            }
            return normalized;
        }

        return null;
    }

    private BusinessException alreadyReportedException() {
        return new BusinessException("ALREADY_REPORTED", "이미 신고한 대상입니다", HttpStatus.BAD_REQUEST);
    }
}
