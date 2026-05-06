package com.rolling.api.domain.map.controller;

import com.rolling.api.domain.map.dto.GeocodeResponse;
import com.rolling.api.domain.map.service.KakaoGeocodeService;
import com.rolling.api.global.exception.AuthException;
import com.rolling.api.global.response.ApiResponse;
import com.rolling.api.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Map", description = "지도/좌표 변환 API")
@RestController
@RequestMapping("/api/v1/maps")
@RequiredArgsConstructor
public class MapController {

    private final KakaoGeocodeService kakaoGeocodeService;

    @Operation(summary = "카카오 주소 좌표 변환", description = "선택된 주소를 카카오 Local API로 위도/경도로 변환합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "좌표 변환 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "주소 입력값 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "좌표를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "카카오 API 호출 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504", description = "카카오 API timeout")
    })
    @GetMapping("/kakao/geocode")
    public ResponseEntity<ApiResponse<GeocodeResponse>> geocode(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "카카오/다음 우편번호 WebView에서 선택된 주소") @RequestParam String address
    ) {
        requireUserId(principal);
        GeocodeResponse response = kakaoGeocodeService.geocode(address);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException("UNAUTHORIZED", "인증이 필요합니다");
        }
        return principal.getUserId();
    }
}
