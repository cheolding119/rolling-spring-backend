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
}
