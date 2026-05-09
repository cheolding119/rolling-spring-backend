package com.rolling.api.domain.community.controller;

import com.rolling.api.domain.community.dto.CommunityCommentCreateRequest;
import com.rolling.api.domain.community.dto.CommunityCommentResponse;
import com.rolling.api.domain.community.dto.CommunityCommentUpdateRequest;
import com.rolling.api.domain.community.dto.CommunityPostCreateRequest;
import com.rolling.api.domain.community.dto.CommunityPostDetailResponse;
import com.rolling.api.domain.community.dto.CommunityPostImageUploadUrlRequest;
import com.rolling.api.domain.community.dto.CommunityPostImageUploadUrlResponse;
import com.rolling.api.domain.community.dto.CommunityReportRequest;
import com.rolling.api.domain.community.dto.CommunityPostSummaryResponse;
import com.rolling.api.domain.community.dto.CommunityPostUpdateRequest;
import com.rolling.api.domain.community.entity.CommunityPostCategory;
import com.rolling.api.domain.community.service.CommunityPostImageUploadService;
import com.rolling.api.domain.community.service.CommunityService;
import com.rolling.api.global.exception.AuthException;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Community", description = "커뮤니티 API")
@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final CommunityPostImageUploadService communityPostImageUploadService;

    @Operation(summary = "게시글 목록 조회", description = "커뮤니티 게시글 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Page<CommunityPostSummaryResponse>>> listPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) CommunityPostCategory category,
            @RequestParam(name = "q", required = false) String keyword,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        Long viewerUserId = principal != null && !principal.isAdmin() ? principal.getUserId() : null;
        Page<CommunityPostSummaryResponse> response = communityService.findPosts(viewerUserId, category, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 게시글 목록 조회", description = "현재 로그인한 사용자가 작성한 커뮤니티 게시글 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/posts/my")
    public ResponseEntity<ApiResponse<Page<CommunityPostSummaryResponse>>> listMyPosts(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) CommunityPostCategory category,
            @RequestParam(name = "q", required = false) String keyword,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<CommunityPostSummaryResponse> response = communityService.findMyPosts(requireUserId(principal), category, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "게시글 상세 조회", description = "커뮤니티 게시글 단건을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @GetMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<CommunityPostDetailResponse>> getPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long viewerUserId = principal != null && !principal.isAdmin() ? principal.getUserId() : null;
        boolean isAdmin = principal != null && principal.isAdmin();
        CommunityPostDetailResponse response = communityService.findPost(viewerUserId, isAdmin, id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "게시글 작성", description = "커뮤니티 게시글을 작성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 유효성 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/posts")
    public ResponseEntity<ApiResponse<CommunityPostDetailResponse>> createPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommunityPostCreateRequest request) {
        CommunityPostDetailResponse response = communityService.createPost(requireUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "게시글 이미지 업로드 URL 발급", description = "커뮤니티 게시글 본문에 첨부할 이미지를 S3에 직접 업로드하기 위한 presigned URL을 발급합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 이미지 형식"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/posts/image-upload-url")
    public ResponseEntity<ApiResponse<CommunityPostImageUploadUrlResponse>> createImageUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CommunityPostImageUploadUrlRequest request) {
        requireUserId(principal);
        CommunityPostImageUploadUrlResponse response = communityPostImageUploadService.createUploadUrl(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "게시글 수정", description = "커뮤니티 게시글을 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 유효성 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PatchMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<CommunityPostDetailResponse>> updatePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CommunityPostUpdateRequest request) {
        CommunityPostDetailResponse response = communityService.updatePost(
                requireUserId(principal),
                principal != null && principal.isAdmin(),
                id,
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "게시글 삭제", description = "커뮤니티 게시글을 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        communityService.deletePost(
                requireUserId(principal),
                principal != null && principal.isAdmin(),
                id
        );
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "게시글 좋아요", description = "게시글에 좋아요를 추가합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/posts/{id}/like")
    public ResponseEntity<ApiResponse<Void>> likePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        communityService.likePost(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "게시글 좋아요 취소", description = "게시글의 좋아요를 취소합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좋아요 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/posts/{id}/like")
    public ResponseEntity<ApiResponse<Void>> unlikePost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        communityService.unlikePost(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "게시글 신고", description = "게시글을 신고합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 유효성 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PostMapping("/posts/{id}/report")
    public ResponseEntity<ApiResponse<Void>> reportPost(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CommunityReportRequest request) {
        communityService.reportPost(requireUserId(principal), id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "댓글 신고", description = "댓글을 신고합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 유효성 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    @PostMapping("/comments/{commentId}/report")
    public ResponseEntity<ApiResponse<Void>> reportComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long commentId,
            @Valid @RequestBody CommunityReportRequest request) {
        communityService.reportComment(requireUserId(principal), commentId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "댓글 목록 조회", description = "게시글의 댓글 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<Page<CommunityCommentResponse>>> listComments(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @PageableDefault(size = 50, sort = {"createdAt", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        Long viewerUserId = principal != null && !principal.isAdmin() ? principal.getUserId() : null;
        Page<CommunityCommentResponse> response = communityService.findComments(
                viewerUserId,
                principal != null && principal.isAdmin(),
                postId,
                pageable
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "댓글 작성", description = "게시글에 댓글을 작성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 유효성 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게시글 없음")
    })
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<CommunityCommentResponse>> createComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long postId,
            @Valid @RequestBody CommunityCommentCreateRequest request) {
        CommunityCommentResponse response = communityService.createComment(requireUserId(principal), postId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "댓글 수정", description = "댓글을 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 유효성 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    @PatchMapping("/comments/{id}")
    public ResponseEntity<ApiResponse<CommunityCommentResponse>> updateComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody CommunityCommentUpdateRequest request) {
        CommunityCommentResponse response = communityService.updateComment(
                requireUserId(principal),
                principal != null && principal.isAdmin(),
                id,
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "댓글 삭제", description = "댓글을 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "댓글 없음")
    })
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        communityService.deleteComment(
                requireUserId(principal),
                principal != null && principal.isAdmin(),
                id
        );
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException("UNAUTHORIZED", "인증이 필요합니다");
        }
        return principal.getUserId();
    }
}
