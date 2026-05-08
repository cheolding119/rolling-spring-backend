package com.rolling.api.domain.community.repository;

import com.rolling.api.domain.community.entity.CommunityPostReport;
import com.rolling.api.domain.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostReportRepository extends JpaRepository<CommunityPostReport, Long> {

    boolean existsByPost_IdAndReporter_Id(Long postId, Long reporterId);

    @EntityGraph(attributePaths = {"reporter", "post", "post.author"})
    Page<CommunityPostReport> findAllByStatus(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "post", "post.author"})
    Page<CommunityPostReport> findAll(Pageable pageable);
}
