package com.rolling.api.domain.tournament.controller;

import com.rolling.api.domain.openmat.entity.Region;
import com.rolling.api.domain.tournament.dto.TournamentCreateRequest;
import com.rolling.api.domain.tournament.dto.TournamentFavoriteReminderUpdateRequest;
import com.rolling.api.domain.tournament.dto.TournamentFavoriteResponse;
import com.rolling.api.domain.tournament.dto.TournamentPosterUploadUrlRequest;
import com.rolling.api.domain.tournament.dto.TournamentPosterUploadUrlResponse;
import com.rolling.api.domain.tournament.dto.TournamentResponse;
import com.rolling.api.domain.tournament.dto.TournamentUpdateRequest;
import com.rolling.api.domain.tournament.entity.TournamentSource;
import com.rolling.api.domain.report.dto.ReportCreateRequest;
import com.rolling.api.domain.tournament.service.TournamentFavoriteService;
import com.rolling.api.domain.tournament.service.TournamentService;
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
import org.springframework.data.web.PageableDefault;
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

@Tag(name = "Tournament", description = "대회 API")
@RestController
@RequestMapping("/api/v1/tournaments")
@RequiredArgsConstructor
public class TournamentController {

    private final TournamentService tournamentService;
    private final TournamentFavoriteService tournamentFavoriteService;

    @Operation(summary = "대회 리스트 조회", description = "대회 목록을 페이징 조회합니다. 접수 가능한 대회가 상단에 정렬되며 source 필터를 지원합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<Page<TournamentResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "출처 필터 (STREET_JIU_JITSU, KOREA_JIU, HEROES_OF_JIU_JITSU, MANUAL)")
            @RequestParam(required = false) TournamentSource source,
            @Parameter(description = "지역 필터 (예: SEOUL, GYEONGGI)")
            @RequestParam(required = false) Region region,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = principal != null ? principal.getUserId() : null;
        Page<TournamentResponse> response = tournamentService.findAll(pageable, source, region, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "대회 상세 조회", description = "대회 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대회 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TournamentResponse>> findById(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        Long userId = principal != null ? principal.getUserId() : null;
        TournamentResponse response = tournamentService.findById(id, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "대회 등록", description = "대회 정보를 등록합니다. 수동 등록 대회는 source가 MANUAL로 저장됩니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<TournamentResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TournamentCreateRequest request) {
        TournamentResponse response = tournamentService.create(requireUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "대회 포스터 업로드 URL 발급", description = "대회 포스터를 S3에 직접 업로드하기 위한 presigned URL을 발급합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 이미지 형식"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/poster-upload-url")
    public ResponseEntity<ApiResponse<TournamentPosterUploadUrlResponse>> createPosterUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TournamentPosterUploadUrlRequest request) {
        requireUserId(principal);
        TournamentPosterUploadUrlResponse response = tournamentService.createPosterUploadUrl(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "대회 수정", description = "작성자 또는 관리자가 대회 정보를 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 또는 관리자 아님")
    })
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TournamentResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody TournamentUpdateRequest request) {
        TournamentResponse response = tournamentService.update(requireUserId(principal), id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "대회 삭제", description = "작성자 본인 대회를 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "작성자 아님")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        tournamentService.delete(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "대회 신고", description = "대회를 신고합니다. 동일 사용자는 같은 대회를 한 번만 신고할 수 있습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "중복 신고/자기 신고/요청 유효성 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대회를 찾을 수 없음")
    })
    @PostMapping("/{id}/report")
    public ResponseEntity<ApiResponse<Void>> report(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody ReportCreateRequest request) {
        tournamentService.report(requireUserId(principal), id, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "대회 찜 추가", description = "로그인 사용자가 대회를 찜합니다. 이미 찜한 경우 멱등적으로 현재 상태를 반환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<TournamentFavoriteResponse>> addFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        TournamentFavoriteResponse response = tournamentFavoriteService.addFavorite(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "대회 찜 해제", description = "로그인 사용자가 대회 찜을 해제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        tournamentFavoriteService.removeFavorite(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "찜한 대회 목록 조회", description = "로그인 사용자가 찜한 대회와 알림 설정 상태를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/favorites")
    public ResponseEntity<ApiResponse<Page<TournamentFavoriteResponse>>> listFavorites(
            @AuthenticationPrincipal UserPrincipal principal,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TournamentFavoriteResponse> response = tournamentFavoriteService.findFavorites(requireUserId(principal), pageable);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "찜한 대회 리마인드 설정", description = "찜한 대회의 알림 on/off와 날짜/시간을 설정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PatchMapping("/{id}/favorite-reminder")
    public ResponseEntity<ApiResponse<TournamentFavoriteResponse>> updateFavoriteReminder(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @RequestBody TournamentFavoriteReminderUpdateRequest request) {
        TournamentFavoriteResponse response =
                tournamentFavoriteService.updateReminder(requireUserId(principal), id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException("UNAUTHORIZED", "인증이 필요합니다");
        }
        return principal.getUserId();
    }
}
