package com.rolling.api.domain.community.repository;

import com.rolling.api.domain.community.entity.CommunityCommentReport;
import com.rolling.api.domain.report.entity.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityCommentReportRepository extends JpaRepository<CommunityCommentReport, Long> {

    boolean existsByComment_IdAndReporter_Id(Long commentId, Long reporterId);

    @EntityGraph(attributePaths = {"reporter", "comment", "comment.post", "comment.post.author", "comment.author"})
    Page<CommunityCommentReport> findAllByStatus(ReportStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"reporter", "comment", "comment.post", "comment.post.author", "comment.author"})
    Page<CommunityCommentReport> findAll(Pageable pageable);
}
