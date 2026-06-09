package com.rolling.api.domain.traininglog.controller;

import com.rolling.api.domain.traininglog.dto.TrainingCardDetailResponse;
import com.rolling.api.domain.traininglog.dto.TrainingCardListItemResponse;
import com.rolling.api.domain.traininglog.entity.TrainingCardLevel;
import com.rolling.api.domain.traininglog.entity.TrainingCardPosition;
import com.rolling.api.domain.traininglog.service.TrainingCardService;
import com.rolling.api.global.exception.AuthException;
import com.rolling.api.global.response.ApiResponse;
import com.rolling.api.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Training Card", description = "훈련카드 조회 API")
@RestController
@RequestMapping("/api/v1/training-logs/me/cards")
@RequiredArgsConstructor
public class TrainingCardController {

    private final TrainingCardService trainingCardService;

    @Operation(summary = "훈련카드 목록 조회", description = "현재 로그인한 사용자가 훈련카드 목록을 검색, 레벨, 포지션 기준으로 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<TrainingCardListItemResponse>>> findCards(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false, name = "q") String query,
            @RequestParam(required = false) TrainingCardLevel level,
            @RequestParam(required = false) TrainingCardPosition position
    ) {
        List<TrainingCardListItemResponse> response = trainingCardService.findCards(
                requireUserId(principal),
                query,
                level,
                position
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "훈련카드 상세 조회", description = "현재 로그인한 사용자가 특정 훈련카드의 상세 설명을 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드 없음")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TrainingCardDetailResponse>> findCardDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        TrainingCardDetailResponse response = trainingCardService.findCardDetail(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "훈련카드 좋아요 추가", description = "현재 로그인한 사용자가 특정 훈련카드에 좋아요를 추가합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드 없음")
    })
    @PostMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> likeCard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        trainingCardService.likeCard(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "훈련카드 좋아요 취소", description = "현재 로그인한 사용자가 특정 훈련카드의 좋아요를 취소합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드 없음")
    })
    @DeleteMapping("/{id}/like")
    public ResponseEntity<ApiResponse<Void>> unlikeCard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        trainingCardService.unlikeCard(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "훈련카드 즐겨찾기 추가", description = "현재 로그인한 사용자가 특정 훈련카드를 즐겨찾기합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드 없음")
    })
    @PostMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Void>> favoriteCard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        trainingCardService.favoriteCard(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "훈련카드 즐겨찾기 취소", description = "현재 로그인한 사용자가 특정 훈련카드의 즐겨찾기를 취소합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "카드 없음")
    })
    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<ApiResponse<Void>> unfavoriteCard(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id
    ) {
        trainingCardService.unfavoriteCard(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException("UNAUTHORIZED", "인증이 필요합니다");
        }
        return principal.getUserId();
    }
}
