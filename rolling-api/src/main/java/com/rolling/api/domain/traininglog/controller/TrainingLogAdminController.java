package com.rolling.api.domain.traininglog.controller;

import com.rolling.api.domain.report.dto.ReportStatusUpdateRequest;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.domain.traininglog.dto.TrainingLogCommentReportAdminResponse;
import com.rolling.api.domain.traininglog.service.TrainingLogSocialService;
import com.rolling.api.global.exception.AuthException;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.response.ApiResponse;
import com.rolling.api.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Training Log Admin", description = "훈련일지 운영자 API")
@RestController
@RequestMapping("/api/v1/admin/training-logs")
@RequiredArgsConstructor
public class TrainingLogAdminController {

    private final TrainingLogSocialService trainingLogSocialService;

    @Operation(summary = "훈련일지 댓글 신고 목록 조회", description = "관리자가 훈련일지 댓글 신고 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/comments/reports")
    public ResponseEntity<ApiResponse<Page<TrainingLogCommentReportAdminResponse>>> listCommentReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(trainingLogSocialService.findCommentReportsForAdmin(status, pageable)));
    }

    @Operation(summary = "훈련일지 댓글 신고 상세 조회", description = "관리자가 훈련일지 댓글 신고 상세를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/comments/reports/{id}")
    public ResponseEntity<ApiResponse<TrainingLogCommentReportAdminResponse>> getCommentReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(trainingLogSocialService.findCommentReportForAdmin(id)));
    }

    @Operation(summary = "훈련일지 댓글 신고 상태 변경", description = "관리자가 훈련일지 댓글 신고 처리 상태를 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/comments/reports/{id}/status")
    public ResponseEntity<ApiResponse<TrainingLogCommentReportAdminResponse>> updateCommentReportStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReportStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                trainingLogSocialService.updateCommentReportStatus(requireAdmin(principal), id, request)
        ));
    }

    private Long requireAdmin(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException("UNAUTHORIZED", "인증이 필요합니다");
        }
        if (!principal.isAdmin()) {
            throw BusinessException.forbidden("관리자 권한이 필요합니다");
        }
        return principal.getUserId();
    }
}
