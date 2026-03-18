package com.rolling.api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "FCM 토큰 등록 요청")
public class UserFcmTokenRequest {

    @NotBlank(message = "fcmToken은 비어 있을 수 없습니다")
    @Schema(description = "FCM 토큰", example = "dK1x...")
    private String fcmToken;

    @Schema(description = "디바이스 플랫폼", example = "ANDROID")
    private String platform;

    @Schema(description = "앱이 관리하는 디바이스 식별자", example = "device-8f4b2a")
    private String deviceId;

    @Schema(description = "앱 버전", example = "1.0.3")
    private String appVersion;
}
