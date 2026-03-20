package com.rolling.api.domain.inquiry.controller;

import com.rolling.api.domain.inquiry.dto.InquiryCreateRequest;
import com.rolling.api.domain.inquiry.dto.InquiryResponse;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Inquiry", description = "사용자 문의 API")
@RestController
@RequestMapping("/api/v1/inquiries")
@RequiredArgsConstructor
public class InquiryController {

    private final InquiryService inquiryService;

    @Operation(summary = "문의 생성", description = "현재 로그인한 사용자가 1:1 문의를 생성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<InquiryResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody InquiryCreateRequest request) {
        InquiryResponse response = inquiryService.create(requireUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 문의 목록 조회", description = "현재 로그인한 사용자의 문의 목록을 최신순으로 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<InquiryResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<InquiryResponse> response = inquiryService.findMyInquiries(requireUserId(principal), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 문의 상세 조회", description = "현재 로그인한 사용자가 본인 문의 상세를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "문의 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InquiryResponse>> findById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        InquiryResponse response = inquiryService.findMyInquiry(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException("UNAUTHORIZED", "인증이 필요합니다");
        }
        return principal.getUserId();
    }
}
