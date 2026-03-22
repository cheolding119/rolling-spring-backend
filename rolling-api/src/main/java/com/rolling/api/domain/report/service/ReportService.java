package com.rolling.api.domain.report.service;

import com.rolling.api.domain.report.entity.Report;
import com.rolling.api.domain.report.entity.ReportReason;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.report.entity.ReportTargetType;
import com.rolling.api.domain.report.repository.ReportRepository;
import com.rolling.api.domain.user.entity.User;
import com.rolling.api.domain.user.repository.UserRepository;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.page.PageableUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.rolling.api.domain.report.dto.ReportResponse;
import com.rolling.api.domain.report.dto.ReportStatusUpdateRequest;
import com.rolling.api.domain.report.dto.ReportTargetSummary;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final long ADMIN_VISIBLE_TARGET_REPORT_THRESHOLD = 3L;
    private static final Sort DEFAULT_ADMIN_SORT = Sort.by(Sort.Direction.DESC, "createdAt");
    private static final Set<String> ALLOWED_ADMIN_SORTS = Set.of("createdAt", "updatedAt", "status", "processedAt", "targetType");

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final Clock clock;

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
                .status(ReportStatus.RECEIVED)
                .build();

        try {
            return reportRepository.saveAndFlush(report);
        } catch (DataIntegrityViolationException e) {
            throw alreadyReportedException();
        }
    }

    @Transactional(readOnly = true)
    public Page<ReportResponse> findAllForAdmin(
            ReportStatus status,
            ReportTargetType targetType,
            LocalDate createdFrom,
            LocalDate createdTo,
            Pageable pageable
    ) {
        validateDateRange(createdFrom, createdTo);
        Page<Report> reports = reportRepository.findAll(
                buildAdminSpecification(
                        status,
                        targetType,
                        toStartOfDay(createdFrom),
                        toExclusiveEndOfDay(createdTo)
                ),
                normalizeAdminPageable(pageable)
        );
        Map<String, ReportTargetSummary> summaryCache = new HashMap<>();
        return reports.map(report -> ReportResponse.from(report, getTargetSummary(report, summaryCache)));
    }

    @Transactional(readOnly = true)
    public ReportResponse findByIdForAdmin(Long reportId) {
        Report report = getReport(reportId);
        return ReportResponse.from(report, getTargetSummary(report, new HashMap<>()));
    }

    @Transactional
    public ReportResponse updateStatus(Long adminUserId, Long reportId, ReportStatusUpdateRequest request) {
        Report report = getReport(reportId);
        report.updateStatus(
                requireStatus(request.getStatus()),
                adminUserId,
                LocalDateTime.now(clock),
                normalizeOptionalText(request.getProcessingMemo()),
                normalizeOptionalText(request.getFinalAction())
        );

        return ReportResponse.from(report, getTargetSummary(report, new HashMap<>()));
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

    private Report getReport(Long reportId) {
        return reportRepository.findVisibleByIdForAdmin(reportId, ADMIN_VISIBLE_TARGET_REPORT_THRESHOLD)
                .orElseThrow(() -> BusinessException.notFound("신고를 찾을 수 없습니다"));
    }

    private ReportStatus requireStatus(ReportStatus status) {
        if (status == null) {
            throw BusinessException.badRequest("신고 상태는 필수입니다");
        }
        return status;
    }

    private void validateDateRange(LocalDate createdFrom, LocalDate createdTo) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw BusinessException.badRequest("createdFrom은 createdTo보다 이후일 수 없습니다");
        }
    }

    private LocalDateTime toStartOfDay(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }

    private LocalDateTime toExclusiveEndOfDay(LocalDate date) {
        return date == null ? null : date.plusDays(1).atStartOfDay();
    }

    private Pageable normalizeAdminPageable(Pageable pageable) {
        return PageableUtils.normalize(pageable, DEFAULT_ADMIN_SORT, ALLOWED_ADMIN_SORTS, 20, 100);
    }

    private Specification<Report> buildAdminSpecification(
            ReportStatus status,
            ReportTargetType targetType,
            LocalDateTime createdFrom,
            LocalDateTime createdToExclusive
    ) {
        return (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();

            var visibleTargets = query.subquery(Long.class);
            var grouped = visibleTargets.from(Report.class);
            visibleTargets.select(grouped.get("targetId"))
                    .where(
                            cb.equal(grouped.get("targetType"), root.get("targetType")),
                            cb.equal(grouped.get("targetId"), root.get("targetId"))
                    )
                    .groupBy(grouped.get("targetType"), grouped.get("targetId"))
                    .having(cb.ge(cb.count(grouped.get("id")), ADMIN_VISIBLE_TARGET_REPORT_THRESHOLD));

            predicates.add(cb.exists(visibleTargets));

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (targetType != null) {
                predicates.add(cb.equal(root.get("targetType"), targetType));
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdToExclusive != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), createdToExclusive));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }

    private String normalizeOptionalText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private ReportTargetSummary getTargetSummary(Report report, Map<String, ReportTargetSummary> summaryCache) {
        String cacheKey = report.getTargetType() + ":" + report.getTargetId();
        return summaryCache.computeIfAbsent(cacheKey, key -> ReportTargetSummary.builder()
                .targetType(report.getTargetType())
                .targetId(report.getTargetId())
                .totalReportCount(reportRepository.countByTargetTypeAndTargetId(report.getTargetType(), report.getTargetId()))
                .receivedCount(reportRepository.countByTargetTypeAndTargetIdAndStatus(report.getTargetType(), report.getTargetId(), ReportStatus.RECEIVED))
                .inReviewCount(reportRepository.countByTargetTypeAndTargetIdAndStatus(report.getTargetType(), report.getTargetId(), ReportStatus.IN_REVIEW))
                .resolvedCount(reportRepository.countByTargetTypeAndTargetIdAndStatus(report.getTargetType(), report.getTargetId(), ReportStatus.RESOLVED))
                .rejectedCount(reportRepository.countByTargetTypeAndTargetIdAndStatus(report.getTargetType(), report.getTargetId(), ReportStatus.REJECTED))
                .build());
    }
}
