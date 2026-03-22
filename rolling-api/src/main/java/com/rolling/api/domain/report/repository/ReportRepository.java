package com.rolling.api.domain.report.repository;

import com.rolling.api.domain.report.entity.Report;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.report.entity.ReportTargetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    boolean existsByReporter_IdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    long countByTargetTypeAndTargetId(ReportTargetType targetType, Long targetId);

    long countByTargetTypeAndTargetIdAndStatus(ReportTargetType targetType, Long targetId, ReportStatus status);

    @Query("""
            select r
            from Report r
            where r.id = :reportId
              and exists (
                select grouped.targetId
                from Report grouped
                where grouped.targetType = r.targetType
                  and grouped.targetId = r.targetId
                group by grouped.targetType, grouped.targetId
                having count(grouped.id) >= :minimumCount
            )
            """)
    Optional<Report> findVisibleByIdForAdmin(@Param("reportId") Long reportId, @Param("minimumCount") long minimumCount);
}
