package com.rolling.api.domain.user.dto;

import com.rolling.api.domain.user.entity.BeltColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "내 정보 수정 요청")
public class UserUpdateRequest {

    @Size(max = 30, message = "닉네임은 30자 이하여야 합니다")
    @Schema(description = "닉네임", example = "rolling_user")
    private String nickname;

    @Schema(description = "주짓수 벨트 색상", example = "BLUE", allowableValues = {"WHITE", "BLUE", "PURPLE", "BROWN", "BLACK"})
    private BeltColor beltColor;
}