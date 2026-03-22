package com.rolling.api.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "로그아웃 요청")
public class LogoutRequest {

    @Schema(
            description = "현재 로그아웃하려는 디바이스의 FCM 토큰. 전달하면 해당 토큰도 함께 제거합니다.",
            example = "dK1x..."
    )
    private String fcmToken;
}
