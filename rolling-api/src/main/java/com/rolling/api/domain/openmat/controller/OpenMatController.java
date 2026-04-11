package com.rolling.api.domain.openmat.controller;

import com.rolling.api.domain.openmat.dto.OpenMatCreateRequest;
import com.rolling.api.domain.openmat.dto.OpenMatHostStatusUpdateRequest;
import com.rolling.api.domain.openmat.dto.OpenMatParticipantResponse;
import com.rolling.api.domain.openmat.dto.OpenMatResponse;
import com.rolling.api.domain.openmat.dto.OpenMatUpdateRequest;
import com.rolling.api.domain.openmat.entity.OpenMatStatus;
import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.openmat.service.OpenMatService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "OpenMat", description = "오픈매트 API")
@RestController
@RequestMapping("/api/v1/open-mats")
@RequiredArgsConstructor
public class OpenMatController {

    private final OpenMatService openMatService;

    @Operation(summary = "오픈매트 생성", description = "새로운 오픈매트를 생성합니다. 인증이 필요합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<OpenMatResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody OpenMatCreateRequest request) {
        OpenMatResponse response = openMatService.create(requireUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내가 신청한 오픈매트 목록", description = "내가 신청한 오픈매트 목록을 조회합니다. 인증이 필요합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<OpenMatResponse>>> myOpenMats(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "startDateTime") Pageable pageable) {
        Page<OpenMatResponse> response = openMatService.findMyOpenMats(requireUserId(principal), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내가 개최한 오픈매트 목록", description = "내가 개최한 오픈매트 목록을 조회합니다. 인증이 필요합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/my-hosting")
    public ResponseEntity<ApiResponse<Page<OpenMatResponse>>> myHostedOpenMats(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 10, sort = "startDateTime") Pageable pageable) {
        Page<OpenMatResponse> response = openMatService.findMyHostedOpenMats(requireUserId(principal), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "오픈매트 목록 조회", description = "오픈매트 목록을 페이징으로 조회합니다. 지역, 상태, 검색어 필터링을 지원합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OpenMatResponse>>> list(
            @Parameter(description = "지역 필터 (예: SEOUL, BUSAN)") @RequestParam(required = false) Region region,
            @Parameter(description = "상태 필터 (RECRUITING, CLOSED, FINISHED)") @RequestParam(required = false) OpenMatStatus status,
            @Parameter(description = "검색어 (제목, 장소명, 주소)") @RequestParam(required = false, name = "q") String keyword,
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        Page<OpenMatResponse> response = openMatService.findAll(region, status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "오픈매트 상세 조회", description = "오픈매트 단건을 상세 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "오픈매트를 찾을 수 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OpenMatResponse>> findById(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "오픈매트 ID") @PathVariable Long id) {
        Long userId = principal != null ? principal.getUserId() : null;
        boolean isAdmin = principal != null && principal.isAdmin();
        OpenMatResponse response = openMatService.findById(id, userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "오픈매트 수정", description = "오픈매트를 수정합니다. 작성자 본인만 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "오픈매트를 찾을 수 없음")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<OpenMatResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "오픈매트 ID") @PathVariable Long id,
            @RequestBody OpenMatUpdateRequest request) {
        Long userId = principal != null ? principal.getUserId() : null;
        OpenMatResponse response = openMatService.update(userId, id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "오픈매트 신청", description = "오픈매트에 참가 신청합니다. 인증이 필요합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신청 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "신청 불가 (마감/종료/중복/정원초과)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "오픈매트를 찾을 수 없음")
    })
    @PostMapping("/{id}/apply")
    public ResponseEntity<ApiResponse<Void>> apply(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "오픈매트 ID") @PathVariable Long id) {
        openMatService.apply(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "오픈매트 신청 취소", description = "오픈매트 참가 신청을 취소합니다. 인증이 필요합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "신청하지 않은 오픈매트"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "오픈매트를 찾을 수 없음")
    })
    @DeleteMapping("/{id}/apply")
    public ResponseEntity<ApiResponse<Void>> cancelApply(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "오픈매트 ID") @PathVariable Long id) {
        openMatService.cancelApply(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "오픈매트 참가자 목록 조회", description = "로그인한 사용자가 오픈매트 참가자 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "오픈매트를 찾을 수 없음")
    })
    @GetMapping("/{id}/participants")
    public ResponseEntity<ApiResponse<List<OpenMatParticipantResponse>>> participants(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "오픈매트 ID") @PathVariable Long id) {
        List<OpenMatParticipantResponse> response = openMatService.findParticipants(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "오픈매트 참가자 강제 취소", description = "오픈매트 작성자가 특정 참가자를 강제로 취소 처리합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "강제 취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "오픈매트 또는 참가자를 찾을 수 없음")
    })
    @DeleteMapping("/{id}/participants/{participantUserId}")
    public ResponseEntity<ApiResponse<Void>> removeParticipant(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "오픈매트 ID") @PathVariable Long id,
            @Parameter(description = "강제 취소할 참가자 사용자 ID") @PathVariable Long participantUserId) {
        openMatService.removeParticipant(requireUserId(principal), id, participantUserId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "오픈매트 모집 상태 수동 변경", description = "오픈매트 작성자가 모집 상태를 RECRUITING 또는 CLOSED로 수동 변경합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "허용되지 않는 상태 변경"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "오픈매트를 찾을 수 없음")
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OpenMatResponse>> updateHostingStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "오픈매트 ID") @PathVariable Long id,
            @Valid @RequestBody OpenMatHostStatusUpdateRequest request) {
        OpenMatResponse response = openMatService.updateHostingStatus(requireUserId(principal), id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "오픈매트 신고",
            description = "오픈매트를 신고합니다. 동일 사용자는 같은 오픈매트를 한 번만 신고할 수 있으며 작성자는 자신의 오픈매트를 신고할 수 없습니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "중복 신고/자기 신고/요청 유효성 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "오픈매트를 찾을 수 없음")
    })
    @PostMapping("/{id}/report")
    public ResponseEntity<ApiResponse<Void>> report(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "오픈매트 ID") @PathVariable Long id,
            @Valid @RequestBody ReportCreateRequest request) {
        openMatService.report(requireUserId(principal), id, request.getReason(), request.getCustomReason());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "오픈매트 삭제", description = "오픈매트를 삭제합니다. 작성자 본인만 가능합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자가 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "오픈매트를 찾을 수 없음")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "오픈매트 ID") @PathVariable Long id) {
        Long userId = principal != null ? principal.getUserId() : null;
        openMatService.delete(userId, id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException("UNAUTHORIZED", "인증이 필요합니다");
        }
        return principal.getUserId();
    }
}



