package com.rolling.api.domain.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "커뮤니티 닉네임 수정 요청")
public class CommunityProfileUpdateRequest {

    @NotBlank(message = "communityNickname은 필수입니다")
    @Size(min = 2, max = 20, message = "communityNickname은 2자 이상 20자 이하여야 합니다")
    @Schema(description = "커뮤니티 전용 닉네임", example = "노기좋아하는거북이")
    private String communityNickname;
}
