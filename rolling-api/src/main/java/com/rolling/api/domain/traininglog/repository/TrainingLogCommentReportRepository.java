package com.rolling.api.domain.traininglog.repository;

import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.traininglog.entity.TrainingLogCommentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

public interface TrainingLogCommentReportRepository extends JpaRepository<TrainingLogCommentReport, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteAllByComment_Entry_Id(Long entryId);

    boolean existsByComment_IdAndReporter_Id(Long commentId, Long reporterId);

    @EntityGraph(attributePaths = {"reporter", "comment", "comment.author", "comment.entry", "comment.entry.user"})
    Page<TrainingLogCommentReport> findAllByStatus(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "comment", "comment.author", "comment.entry", "comment.entry.user"})
    Page<TrainingLogCommentReport> findAll(Pageable pageable);
}
