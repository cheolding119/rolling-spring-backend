package com.rolling.api.domain.community.controller;

import com.rolling.api.domain.community.dto.CommunityAdminCommentResponse;
import com.rolling.api.domain.community.dto.CommunityAdminPostResponse;
import com.rolling.api.domain.community.dto.CommunityCommentReportAdminResponse;
import com.rolling.api.domain.community.dto.CommunityPostReportAdminResponse;
import com.rolling.api.domain.community.entity.CommunityCommentStatus;
import com.rolling.api.domain.community.entity.CommunityPostStatus;
import com.rolling.api.domain.community.service.CommunityService;
import com.rolling.api.domain.report.dto.ReportStatusUpdateRequest;
import com.rolling.api.domain.report.entity.ReportStatus;
import com.rolling.api.global.exception.AuthException;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.response.ApiResponse;
import com.rolling.api.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Community Admin", description = "커뮤니티 운영자 API")
@RestController
@RequestMapping("/api/v1/admin/community")
@RequiredArgsConstructor
public class CommunityAdminController {

    private final CommunityService communityService;

    @Operation(summary = "게시글 목록 조회", description = "관리자가 커뮤니티 게시글 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Page<CommunityAdminPostResponse>>> listPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) CommunityPostStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(communityService.findPostsForAdmin(status, q, pageable)));
    }

    @Operation(summary = "게시글 상세 조회", description = "관리자가 커뮤니티 게시글 상세를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<CommunityAdminPostResponse>> getPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(communityService.findPostForAdmin(id)));
    }

    @Operation(summary = "게시글 숨김", description = "관리자가 커뮤니티 게시글을 숨김 처리합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/posts/{id}/hide")
    public ResponseEntity<ApiResponse<CommunityAdminPostResponse>> hidePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(communityService.hidePost(requireAdmin(principal), id)));
    }

    @Operation(summary = "게시글 숨김 해제", description = "관리자가 커뮤니티 게시글 숨김을 해제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/posts/{id}/unhide")
    public ResponseEntity<ApiResponse<CommunityAdminPostResponse>> unhidePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(communityService.unhidePost(requireAdmin(principal), id)));
    }

    @Operation(summary = "댓글 목록 조회", description = "관리자가 커뮤니티 댓글 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/comments")
    public ResponseEntity<ApiResponse<Page<CommunityAdminCommentResponse>>> listComments(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Long postId,
            @RequestParam(required = false) CommunityCommentStatus status,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(communityService.findCommentsForAdmin(postId, status, q, pageable)));
    }

    @Operation(summary = "댓글 상세 조회", description = "관리자가 커뮤니티 댓글 상세를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/comments/{id}")
    public ResponseEntity<ApiResponse<CommunityAdminCommentResponse>> getComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(communityService.findCommentForAdmin(id)));
    }

    @Operation(summary = "댓글 숨김", description = "관리자가 커뮤니티 댓글을 숨김 처리합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/comments/{id}/hide")
    public ResponseEntity<ApiResponse<CommunityAdminCommentResponse>> hideComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(communityService.hideComment(requireAdmin(principal), id)));
    }

    @Operation(summary = "댓글 숨김 해제", description = "관리자가 커뮤니티 댓글 숨김을 해제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/comments/{id}/unhide")
    public ResponseEntity<ApiResponse<CommunityAdminCommentResponse>> unhideComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(communityService.unhideComment(requireAdmin(principal), id)));
    }

    @Operation(summary = "게시글 신고 목록 조회", description = "관리자가 커뮤니티 게시글 신고 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/posts/reports")
    public ResponseEntity<ApiResponse<Page<CommunityPostReportAdminResponse>>> listPostReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(communityService.findPostReportsForAdmin(status, pageable)));
    }

    @Operation(summary = "댓글 신고 목록 조회", description = "관리자가 커뮤니티 댓글 신고 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/comments/reports")
    public ResponseEntity<ApiResponse<Page<CommunityCommentReportAdminResponse>>> listCommentReports(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(communityService.findCommentReportsForAdmin(status, pageable)));
    }

    @Operation(summary = "게시글 신고 상태 변경", description = "관리자가 커뮤니티 게시글 신고 처리 상태를 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/posts/reports/{id}/status")
    public ResponseEntity<ApiResponse<CommunityPostReportAdminResponse>> updatePostReportStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReportStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(communityService.updatePostReportStatus(requireAdmin(principal), id, request)));
    }

    @Operation(summary = "댓글 신고 상태 변경", description = "관리자가 커뮤니티 댓글 신고 처리 상태를 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/comments/reports/{id}/status")
    public ResponseEntity<ApiResponse<CommunityCommentReportAdminResponse>> updateCommentReportStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReportStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(communityService.updateCommentReportStatus(requireAdmin(principal), id, request)));
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
