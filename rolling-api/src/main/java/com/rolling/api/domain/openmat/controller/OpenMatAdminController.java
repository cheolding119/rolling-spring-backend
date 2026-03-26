package com.rolling.api.domain.openmat.controller;

import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.service.OpenMatService;
import com.rolling.api.global.exception.AuthException;
import com.rolling.api.global.exception.BusinessException;
import com.rolling.api.global.response.ApiResponse;
import com.rolling.api.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "OpenMat", description = "관리자 오픈매트 운영 API")
@RestController
@RequestMapping("/api/v1/admin/open-mats")
@RequiredArgsConstructor
public class OpenMatAdminController {

    private final OpenMatService openMatService;

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
