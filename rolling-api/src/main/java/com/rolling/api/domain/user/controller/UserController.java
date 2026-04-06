package com.rolling.api.domain.user.controller;

import com.rolling.api.domain.user.dto.UserFcmTokenDeleteRequest;
import com.rolling.api.domain.user.dto.UserFcmTokenRequest;
import com.rolling.api.domain.user.dto.UserResponse;
import com.rolling.api.domain.user.dto.UserSettingsResponse;
import com.rolling.api.domain.user.dto.UserSettingsUpdateRequest;
import com.rolling.api.domain.user.dto.UserUpdateRequest;
import com.rolling.api.domain.user.service.UserService;
import com.rolling.api.global.exception.AuthException;
import com.rolling.api.global.response.ApiResponse;
import com.rolling.api.global.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User", description = "사용자 API")
@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "내 정보 조회",
            description = "JWT Access Token에서 userId를 추출하여 내 정보를 조회합니다"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        UserResponse response = userService.getMe(requireUserId(principal));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "내 정보 수정",
            description = "닉네임 또는 벨트 색상을 수정합니다. 요청 검증은 수행하지 않습니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateMe(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UserUpdateRequest request) {
        UserResponse response = userService.updateMe(requireUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "내 설정 수정",
            description = "현재 로그인한 사용자의 설정을 부분 수정합니다. 현재는 푸시 알림 수신 여부만 지원합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 유효성 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PatchMapping("/me/settings")
    public ResponseEntity<ApiResponse<UserSettingsResponse>> updateSettings(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserSettingsUpdateRequest request) {
        UserSettingsResponse response = userService.updateSettings(requireUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(
            summary = "FCM 토큰 등록",
            description = "현재 로그인한 사용자 FCM 토큰을 등록/갱신합니다. 동일 토큰이 이미 다른 사용자에게 연결돼 있으면 현재 사용자에게 재연결하고 디바이스 메타데이터를 최신값으로 갱신합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 유효성 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/me/fcm")
    public ResponseEntity<ApiResponse<Void>> registerFcmToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserFcmTokenRequest request) {
        userService.registerFcmToken(requireUserId(principal), request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(
            summary = "FCM 토큰 삭제",
            description = "현재 로그인한 사용자에게 연결된 현재 디바이스 FCM 토큰을 명시적으로 제거합니다. 토큰이 없어도 성공으로 처리합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 유효성 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @DeleteMapping("/me/fcm")
    public ResponseEntity<ApiResponse<Void>> unregisterFcmToken(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody UserFcmTokenDeleteRequest request) {
        userService.unregisterFcmToken(requireUserId(principal), request.getFcmToken());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(
            summary = "사용자 차단",
            description = "현재 로그인한 사용자가 특정 사용자를 차단합니다. 이미 차단된 경우에도 성공 응답을 반환합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "차단 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "자기 자신 차단 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 사용자 없음")
    })
    @PostMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> blockUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        userService.blockUser(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(
            summary = "사용자 차단 해제",
            description = "현재 로그인한 사용자가 특정 사용자 차단을 해제합니다. 차단 상태가 아니어도 성공 응답을 반환합니다."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "해제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "자기 자신 차단 해제 시도"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "대상 사용자 없음")
    })
    @DeleteMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Void>> unblockUser(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id) {
        userService.unblockUser(requireUserId(principal), id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private Long requireUserId(UserPrincipal principal) {
        if (principal == null) {
            throw new AuthException("UNAUTHORIZED", "인증이 필요합니다");
        }
        return principal.getUserId();
    }
}
