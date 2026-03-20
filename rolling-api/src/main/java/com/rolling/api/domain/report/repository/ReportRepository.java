package com.rolling.api.domain.report.repository;

import com.rolling.api.domain.report.entity.Report;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.report.entity.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporter_IdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);

    long countByTargetTypeAndTargetIdAndStatus(ReportTargetType targetType, Long targetId, ReportStatus status);
}
