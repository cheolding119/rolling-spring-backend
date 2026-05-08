package com.rolling.api.domain.community.repository;

import com.rolling.api.domain.community.entity.CommunityCommentReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityCommentReportRepository extends JpaRepository<CommunityCommentReport, Long> {

    boolean existsByComment_IdAndReporter_Id(Long commentId, Long reporterId);
}
