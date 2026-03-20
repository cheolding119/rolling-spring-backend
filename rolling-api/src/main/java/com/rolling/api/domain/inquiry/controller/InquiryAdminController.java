package com.rolling.api.domain.inquiry.controller;

import com.rolling.api.domain.inquiry.dto.InquiryAnswerRequest;
import com.rolling.api.domain.inquiry.dto.InquiryResponse;
import com.rolling.api.domain.inquiry.dto.InquiryStatusUpdateRequest;
import com.rolling.api.domain.inquiry.service.InquiryService;
import com.rolling.api.global.exception.AuthException;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inquiry", description = "관리자 문의 운영 API")
@RestController
@RequestMapping("/api/v1/admin/inquiries")
@RequiredArgsConstructor
public class InquiryAdminController {

    private final InquiryService inquiryService;

    @Operation(summary = "문의 목록 조회", description = "ADMIN 권한 사용자가 전체 문의 목록을 최신순으로 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InquiryResponse>>> list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InquiryResponse> response = inquiryService.findAllForAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "문의 상세 조회", description = "ADMIN 권한 사용자가 문의 상세를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "문의 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InquiryResponse>> findById(@PathVariable Long id) {
        InquiryResponse response = inquiryService.findByIdForAdmin(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "문의 답변 저장", description = "ADMIN 권한 사용자가 문의 답변을 저장하고 상태를 ANSWERED로 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "문의 없음")
    })
    @PatchMapping("/{id}/answer")
    public ResponseEntity<ApiResponse<InquiryResponse>> answer(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody InquiryAnswerRequest request) {
        InquiryResponse response = inquiryService.answer(requireUserId(principal), id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "문의 상태 변경", description = "ADMIN 권한 사용자가 문의 상태를 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "상태 변경 불가"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "문의 없음")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<InquiryResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody InquiryStatusUpdateRequest request) {
        InquiryResponse response = inquiryService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException("UNAUTHORIZED", "인증이 필요합니다");
        }
        return principal.getUserId();
    }
}
