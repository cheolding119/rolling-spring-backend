package com.rolling.api.domain.openmat.repository;

import com.rolling.api.domain.openmat.entity.OpenMatCommentReport;
import com.rolling.api.domain.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OpenMatCommentReportRepository extends JpaRepository<OpenMatCommentReport, Long> {

    boolean existsByComment_IdAndReporter_Id(Long commentId, Long reporterId);

    @EntityGraph(attributePaths = {"reporter", "comment", "comment.author", "comment.openMat", "comment.openMat.host", "comment.parentComment"})
    Page<OpenMatCommentReport> findAllByStatus(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "comment", "comment.author", "comment.openMat", "comment.openMat.host", "comment.parentComment"})
    Page<OpenMatCommentReport> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"reporter", "comment", "comment.author", "comment.openMat", "comment.openMat.host", "comment.parentComment"})
    Optional<OpenMatCommentReport> findById(Long id);
}
