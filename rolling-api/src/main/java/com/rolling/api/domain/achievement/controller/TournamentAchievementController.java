package com.rolling.api.domain.achievement.controller;

import com.rolling.api.domain.achievement.dto.*;
import com.rolling.api.domain.achievement.service.TournamentAchievementService;
import com.rolling.api.global.exception.AuthException;
import com.rolling.api.global.response.ApiResponse;
import com.rolling.api.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tournament-achievements/me")
@RequiredArgsConstructor
@Tag(name = "Tournament Achievement", description = "본인 대회 성적 및 메달 API")
@SecurityRequirement(name = "bearerAuth")
public class TournamentAchievementController {
    private final TournamentAchievementService service;

    @GetMapping
    @Operation(summary = "본인 대회 성적 목록 조회")
    public ResponseEntity<ApiResponse<Page<TournamentAchievementResponse>>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.success(service.list(requireUserId(principal), page, size)));
    }

    @GetMapping("/summary")
    @Operation(summary = "본인 메달 집계 조회")
    public ResponseEntity<ApiResponse<TournamentAchievementSummaryResponse>> summary(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.success(service.summary(requireUserId(principal))));
    }

    @GetMapping("/{id}")
    @Operation(summary = "본인 대회 성적 상세 조회")
    public ResponseEntity<ApiResponse<TournamentAchievementResponse>> get(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.get(requireUserId(principal), id)));
    }

    @PostMapping
    @Operation(summary = "대회 성적 등록")
    public ResponseEntity<ApiResponse<TournamentAchievementResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TournamentAchievementSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.create(requireUserId(principal), request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "본인 대회 성적 전체 수정")
    public ResponseEntity<ApiResponse<TournamentAchievementResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id,
            @Valid @RequestBody TournamentAchievementSaveRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.update(requireUserId(principal), id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "본인 대회 성적 삭제")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        service.delete(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> invalidBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiResponse.error("VALIDATION_ERROR",
                "요청 본문, 날짜 또는 선택값 형식이 올바르지 않습니다"));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException("UNAUTHORIZED", "인증이 필요합니다");
        }
        return principal.getUserId();
    }
}
