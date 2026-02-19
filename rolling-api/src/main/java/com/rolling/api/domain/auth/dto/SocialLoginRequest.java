package com.rolling.api.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "소셜 로그인 요청")
public class SocialLoginRequest {

    @NotBlank(message = "provider는 필수입니다")
    @Schema(description = "소셜 로그인 제공자", example = "GOOGLE", allowableValues = {"KAKAO", "GOOGLE"})
    private String provider;

    @NotBlank(message = "accessToken은 필수입니다")
    @Schema(description = "소셜 로그인 제공자에서 발급받은 Access Token", example = "ya29.a0AfH6SMB...")
    private String accessToken;
}