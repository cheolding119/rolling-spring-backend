package com.rolling.api.domain.report.repository;

import com.rolling.api.domain.report.entity.Report;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.report.entity.ReportTargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReportRepository extends JpaRepository<Report, Long>, JpaSpecificationExecutor<Report> {

    boolean existsByReporter_IdAndTargetTypeAndTargetId(Long reporterId, ReportTargetType targetType, Long targetId);

    @Override
    @EntityGraph(attributePaths = "reporter")
    Page<Report> findAll(Specification<Report> spec, Pageable pageable);

    @Query("""
            select r.targetType as targetType,
                   r.targetId as targetId,
                   count(r) as totalReportCount,
                   sum(case when r.status = com.rolling.api.domain.report.entity.ReportStatus.RECEIVED then 1 else 0 end) as receivedCount,
                   sum(case when r.status = com.rolling.api.domain.report.entity.ReportStatus.IN_REVIEW then 1 else 0 end) as inReviewCount,
                   sum(case when r.status = com.rolling.api.domain.report.entity.ReportStatus.RESOLVED then 1 else 0 end) as resolvedCount,
                   sum(case when r.status = com.rolling.api.domain.report.entity.ReportStatus.REJECTED then 1 else 0 end) as rejectedCount
            from Report r
            where r.targetType in :targetTypes
              and r.targetId in :targetIds
            group by r.targetType, r.targetId
            """)
    List<ReportTargetSummaryView> summarizeTargets(
            @Param("targetTypes") Collection<ReportTargetType> targetTypes,
            @Param("targetIds") Collection<Long> targetIds
    );

    @EntityGraph(attributePaths = "reporter")
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

    interface ReportTargetSummaryView {
        ReportTargetType getTargetType();

        Long getTargetId();

        Long getTotalReportCount();

        Long getReceivedCount();

        Long getInReviewCount();

        Long getResolvedCount();

        Long getRejectedCount();
    }
}
