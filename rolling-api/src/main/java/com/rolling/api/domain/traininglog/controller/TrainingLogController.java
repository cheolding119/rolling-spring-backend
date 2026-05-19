package com.rolling.api.domain.traininglog.controller;

import com.rolling.api.domain.traininglog.dto.TrainingLogEntryCreateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntrySummaryResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogEntryUpdateRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogMonthlyCalendarResponse;
import com.rolling.api.domain.traininglog.dto.TrainingLogImageUploadUrlRequest;
import com.rolling.api.domain.traininglog.dto.TrainingLogImageUploadUrlResponse;
import com.rolling.api.domain.traininglog.service.TrainingLogImageUploadService;
import com.rolling.api.domain.traininglog.service.TrainingLogService;
import com.rolling.api.global.exception.AuthException;
import com.rolling.api.global.response.ApiResponse;
import com.rolling.api.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.util.List;

@Tag(name = "Training Log", description = "개인 훈련 기록 API")
@RestController
@RequestMapping("/api/v1/training-logs/me")
@RequiredArgsConstructor
public class TrainingLogController {

    private final TrainingLogService trainingLogService;
    private final TrainingLogImageUploadService trainingLogImageUploadService;

    @Operation(summary = "특정 날짜 훈련 기록 요약 카드 목록 조회", description = "현재 로그인한 사용자의 특정 날짜 훈련 기록 요약 카드 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/entries")
    public ResponseEntity<ApiResponse<List<TrainingLogEntrySummaryResponse>>> findEntries(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        List<TrainingLogEntrySummaryResponse> response = trainingLogService.findEntrySummaries(requireUserId(principal), date);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "특정 날짜 훈련 기록 생성", description = "현재 로그인한 사용자가 특정 날짜에 훈련 기록을 생성합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/entries/{date}")
    public ResponseEntity<ApiResponse<TrainingLogEntryResponse>> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody TrainingLogEntryCreateRequest request
    ) {
        TrainingLogEntryResponse response = trainingLogService.create(requireUserId(principal), date, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "훈련 기록 상세 조회", description = "현재 로그인한 사용자의 특정 훈련 기록 상세를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 기록이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기록 없음")
    })
    @GetMapping("/entries/{id}")
    public ResponseEntity<ApiResponse<TrainingLogEntryResponse>> findEntryDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        TrainingLogEntryResponse response = trainingLogService.findEntryDetail(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "훈련 기록 수정", description = "현재 로그인한 사용자가 본인 훈련 기록을 수정합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 기록이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기록 없음")
    })
    @PatchMapping("/entries/{id}")
    public ResponseEntity<ApiResponse<TrainingLogEntryResponse>> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody TrainingLogEntryUpdateRequest request
    ) {
        TrainingLogEntryResponse response = trainingLogService.update(requireUserId(principal), id, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "훈련 기록 삭제", description = "현재 로그인한 사용자가 본인 훈련 기록을 삭제합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 기록이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "기록 없음")
    })
    @DeleteMapping("/entries/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        trainingLogService.delete(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "해시태그 자동완성", description = "현재 로그인한 사용자의 훈련 기록 해시태그를 기준으로 자동완성 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "검색어 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/tags")
    public ResponseEntity<ApiResponse<List<String>>> autocompleteTags(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam String q
    ) {
        List<String> response = trainingLogService.autocompleteTags(requireUserId(principal), q);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "훈련 기록 월간 캘린더 요약 조회", description = "현재 로그인한 사용자의 월간 훈련 기록 요약을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/calendar")
    public ResponseEntity<ApiResponse<TrainingLogMonthlyCalendarResponse>> calendar(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam int year,
            @RequestParam int month
    ) {
        TrainingLogMonthlyCalendarResponse response = trainingLogService.getMonthlyCalendarSummary(requireUserId(principal), year, month);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "최근 훈련 기록 조회", description = "현재 로그인한 사용자의 최근 훈련 기록 목록을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/recent")
    public ResponseEntity<ApiResponse<List<TrainingLogEntryResponse>>> recent(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<TrainingLogEntryResponse> response = trainingLogService.findRecentEntries(requireUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "훈련 기록 이미지 업로드 URL 발급", description = "현재 로그인한 사용자가 훈련 기록 이미지 업로드용 presigned URL을 발급받습니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/upload-url")
    public ResponseEntity<ApiResponse<TrainingLogImageUploadUrlResponse>> createUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TrainingLogImageUploadUrlRequest request
    ) {
        requireUserId(principal);
        TrainingLogImageUploadUrlResponse response = trainingLogImageUploadService.createUploadUrl(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException("UNAUTHORIZED", "인증이 필요합니다");
        }
        return principal.getUserId();
    }
}
