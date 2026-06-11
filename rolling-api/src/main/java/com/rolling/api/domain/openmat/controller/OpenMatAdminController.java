package com.rolling.api.domain.openmat.controller;

import com.rolling.api.domain.openmat.dto.OpenMatCommentReportAdminResponse;
import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.service.OpenMatService;
import com.rolling.api.domain.openmat.service.OpenMatCommentService;
import com.rolling.api.domain.report.dto.ReportStatusUpdateRequest;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.global.exception.AuthException;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.response.ApiResponse;
import com.rolling.api.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OpenMat", description = "관리자 오픈매트 운영 API")
@RestController
@RequestMapping("/api/v1/admin/open-mats")
@RequiredArgsConstructor
public class OpenMatAdminController {

    private final OpenMatService openMatService;
    private final OpenMatCommentService openMatCommentService;

    @Operation(
            summary = "오픈매트 신고 차단 해제",
            description = "신고 누적으로 신청이 차단된 오픈매트의 차단을 해제합니다. 내부 신고 누적 수를 0으로 초기화합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "오픈매트를 찾을 수 없음")
    })
    @PatchMapping("/{id}/report-block")
    public ResponseEntity<ApiResponse<OpenMatResponse>> clearReportBlock(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long adminUserId = requireAdmin(principal);
        OpenMatResponse response = openMatService.clearReportBlock(adminUserId, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "오픈매트 댓글 신고 목록 조회", description = "관리자가 오픈매트 댓글 신고 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/comments/reports")
    public ResponseEntity<ApiResponse<Page<OpenMatCommentReportAdminResponse>>> listCommentReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(openMatCommentService.findCommentReportsForAdmin(status, pageable)));
    }

    @Operation(summary = "오픈매트 댓글 신고 상세 조회", description = "관리자가 오픈매트 댓글 신고 상세를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/comments/reports/{id}")
    public ResponseEntity<ApiResponse<OpenMatCommentReportAdminResponse>> getCommentReport(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(openMatCommentService.findCommentReportForAdmin(id)));
    }

    @Operation(summary = "오픈매트 댓글 신고 상태 변경", description = "관리자가 오픈매트 댓글 신고 처리 상태를 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/comments/reports/{id}/status")
    public ResponseEntity<ApiResponse<OpenMatCommentReportAdminResponse>> updateCommentReportStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReportStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                openMatCommentService.updateCommentReportStatus(requireAdmin(principal), id, request)
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
