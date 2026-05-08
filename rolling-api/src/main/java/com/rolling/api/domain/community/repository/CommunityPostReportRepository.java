package com.rolling.api.domain.community.repository;

import com.rolling.api.domain.community.entity.CommunityPostReport;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommunityPostReportRepository extends JpaRepository<CommunityPostReport, Long> {

    boolean existsByPost_IdAndReporter_Id(Long postId, Long reporterId);
}
