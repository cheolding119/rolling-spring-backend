package com.rolling.api.domain.seminar.controller;

import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.seminar.dto.SeminarApplicationResponse;
import com.rolling.api.domain.seminar.dto.SeminarCancelApplicationRequest;
import com.rolling.api.domain.seminar.dto.SeminarCreateRequest;
import com.rolling.api.domain.seminar.dto.SeminarHostCancelApplicationRequest;
import com.rolling.api.domain.seminar.dto.SeminarResponse;
import com.rolling.api.domain.seminar.dto.SeminarStatusUpdateRequest;
import com.rolling.api.domain.seminar.dto.SeminarUpdateRequest;
import com.rolling.api.domain.seminar.entity.SeminarApplicationStatus;
import com.rolling.api.domain.seminar.entity.SeminarStatus;
import com.rolling.api.domain.seminar.service.SeminarService;
import com.rolling.api.domain.report.dto.ReportCreateRequest;
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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@Tag(name = "Seminar", description = "세미나 API")
@RestController
@RequestMapping("/api/v1/seminars")
@RequiredArgsConstructor
public class SeminarController {

    private final SeminarService seminarService;

    @Operation(summary = "세미나 생성", description = "새로운 세미나를 생성합니다. 인증이 필요합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<SeminarResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody SeminarCreateRequest request
    ) {
        SeminarResponse response = seminarService.create(requireUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 신청 세미나 목록", description = "내가 신청한 세미나 목록을 조회합니다. 인증이 필요합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my-applications")
    public ResponseEntity<ApiResponse<Page<SeminarResponse>>> myApplications(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) SeminarApplicationStatus status,
            @PageableDefault(size = 10, sort = "seminar.startDateTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<SeminarResponse> response = seminarService.findMyApplications(requireUserId(principal), status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 주최 세미나 목록", description = "내가 주최한 세미나 목록을 조회합니다. 인증이 필요합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<SeminarResponse>>> myHostedSeminars(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) SeminarStatus status,
            @PageableDefault(size = 10, sort = "startDateTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<SeminarResponse> response = seminarService.findMyHostedSeminars(requireUserId(principal), status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "세미나 목록 조회", description = "세미나 목록을 페이징으로 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<SeminarResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "지역 필터") @RequestParam(required = false) Region region,
            @Parameter(description = "상태 필터") @RequestParam(required = false) SeminarStatus status,
            @Parameter(description = "검색어") @RequestParam(required = false, name = "q") String keyword,
            @Parameter(description = "시작 일시 하한") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "시작 일시 상한") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @PageableDefault(size = 20, sort = "startDateTime", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Long userId = principal != null ? principal.getUserId() : null;
        Page<SeminarResponse> response = seminarService.findAll(region, status, keyword, from, to, pageable, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "세미나 상세 조회", description = "세미나 단건을 상세 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<SeminarResponse>> findById(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "세미나 ID") @PathVariable Long id
    ) {
        Long userId = principal != null ? principal.getUserId() : null;
        SeminarResponse response = seminarService.findById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "세미나 수정", description = "세미나를 수정합니다. 호스트만 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SeminarResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "세미나 ID") @PathVariable Long id,
            @Valid @RequestBody SeminarUpdateRequest request
    ) {
        SeminarResponse response = seminarService.update(requireUserId(principal), id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "세미나 삭제", description = "세미나를 삭제합니다. 호스트만 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "세미나 ID") @PathVariable Long id
    ) {
        seminarService.delete(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "세미나 참석 신청", description = "세미나 참석을 신청합니다. 인증이 필요합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/applications")
    public ResponseEntity<ApiResponse<SeminarApplicationResponse>> apply(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "세미나 ID") @PathVariable Long id
    ) {
        SeminarApplicationResponse response = seminarService.apply(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "세미나 참석 신청 취소", description = "내 세미나 참석 신청을 취소합니다. 인증이 필요합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}/applications/me")
    public ResponseEntity<ApiResponse<SeminarApplicationResponse>> cancelMyApplication(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "세미나 ID") @PathVariable Long id,
            @RequestBody(required = false) SeminarCancelApplicationRequest request
    ) {
        SeminarApplicationResponse response = seminarService.cancelMyApplication(requireUserId(principal), id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "세미나 신청자 목록 조회", description = "호스트가 세미나 신청자 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}/applications")
    public ResponseEntity<ApiResponse<Page<SeminarApplicationResponse>>> applications(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "세미나 ID") @PathVariable Long id,
            @RequestParam(required = false) SeminarApplicationStatus status,
            @PageableDefault(size = 20, sort = "appliedAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Page<SeminarApplicationResponse> response = seminarService.findApplications(requireUserId(principal), id, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "세미나 참가자 강제 취소", description = "호스트가 특정 신청자를 강제 취소합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/applications/{applicationId}/cancel")
    public ResponseEntity<ApiResponse<SeminarApplicationResponse>> cancelApplicationByHost(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "세미나 ID") @PathVariable Long id,
            @Parameter(description = "신청 ID") @PathVariable Long applicationId,
            @RequestBody(required = false) SeminarHostCancelApplicationRequest request
    ) {
        SeminarApplicationResponse response = seminarService.cancelApplicationByHost(
                requireUserId(principal),
                id,
                applicationId,
                request
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "세미나 모집 상태 변경", description = "호스트가 세미나 모집 상태를 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<SeminarResponse>> updateStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "세미나 ID") @PathVariable Long id,
            @Valid @RequestBody SeminarStatusUpdateRequest request
    ) {
        SeminarResponse response = seminarService.updateStatus(requireUserId(principal), id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "세미나 신고", description = "세미나를 신고합니다. 동일 사용자는 같은 세미나를 한 번만 신고할 수 있습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/reports")
    public ResponseEntity<ApiResponse<Void>> report(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "세미나 ID") @PathVariable Long id,
            @Valid @RequestBody ReportCreateRequest request
    ) {
        seminarService.report(requireUserId(principal), id, request.getReason(), request.getCustomReason());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException("UNAUTHORIZED", "인증이 필요합니다");
        }
        return principal.getUserId();
    }
}
