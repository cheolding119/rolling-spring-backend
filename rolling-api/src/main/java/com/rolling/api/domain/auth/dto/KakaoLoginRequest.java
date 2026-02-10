package com.rolling.api.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoLoginRequest {

    @NotBlank(message = "provider는 필수입니다")
    private String provider;

    @NotBlank(message = "accessToken은 필수입니다")
    private String accessToken;
}
