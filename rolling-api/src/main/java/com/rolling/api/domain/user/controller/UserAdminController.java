package com.rolling.api.domain.user.controller;

import com.rolling.api.domain.user.dto.AdminUserDetailResponse;
import com.rolling.api.domain.user.dto.AdminUserSummaryResponse;
import com.rolling.api.domain.user.dto.UserSanctionCreateRequest;
import com.rolling.api.domain.user.dto.UserSanctionResponse;
import com.rolling.api.domain.user.entity.AccountStatus;
import com.rolling.api.domain.user.service.UserAdminService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "관리자 사용자 운영 API")
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserAdminController {

    private final UserAdminService userAdminService;

    @Operation(summary = "사용자 목록 조회", description = "ADMIN 권한 사용자가 사용자 목록을 검색하고 계정 상태를 확인합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminUserSummaryResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "닉네임/이메일/소속/전화/ID 검색어")
            @RequestParam(required = false) String q,
            @Parameter(description = "계정 상태 필터 (ACTIVE, WARNING, SUSPENDED, BANNED, WITHDRAWN)")
            @RequestParam(required = false) AccountStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(userAdminService.findUsers(q, status, pageable)));
    }

    @Operation(summary = "사용자 상세 조회", description = "ADMIN 권한 사용자가 사용자 상세와 제재 상태를 확인합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminUserDetailResponse>> findById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(userAdminService.findUser(id)));
    }

    @Operation(summary = "사용자 제재 이력 조회", description = "ADMIN 권한 사용자가 특정 사용자의 제재 이력을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @GetMapping("/{id}/sanctions")
    public ResponseEntity<ApiResponse<java.util.List<UserSanctionResponse>>> listSanctions(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(userAdminService.findSanctions(id)));
    }

    @Operation(summary = "사용자 제재 생성", description = "ADMIN 권한 사용자가 경고, 일시정지, 영구정지를 부여합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @PostMapping("/{id}/sanctions")
    public ResponseEntity<ApiResponse<UserSanctionResponse>> createSanction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody UserSanctionCreateRequest request) {
        Long adminUserId = requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(userAdminService.createSanction(adminUserId, id, request)));
    }

    @Operation(summary = "사용자 제재 해제", description = "ADMIN 권한 사용자가 현재 활성 제재를 해제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "해제 불가"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자 없음")
    })
    @DeleteMapping("/{id}/sanctions/{sanctionId}")
    public ResponseEntity<ApiResponse<UserSanctionResponse>> releaseSanction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @PathVariable Long sanctionId) {
        Long adminUserId = requireAdmin(principal);
        return ResponseEntity.ok(ApiResponse.success(userAdminService.releaseSanction(adminUserId, id, sanctionId)));
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
