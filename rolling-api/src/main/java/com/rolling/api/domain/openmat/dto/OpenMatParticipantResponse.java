package com.rolling.api.domain.openmat.dto;

import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "오픈매트 참가자 정보")
public class OpenMatParticipantResponse {

    @Schema(description = "참가자 사용자 ID", example = "12")
    private Long userId;

    @Schema(description = "참가자 닉네임", example = "rolling_user")
    private String nickname;

    @Schema(description = "참가자 벨트", example = "BLUE")
    private BeltColor beltColor;

    @Schema(description = "참가자 연락처", example = "010-1234-5678")
    private String phone;

    public static OpenMatParticipantResponse from(User user) {
        return OpenMatParticipantResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .beltColor(user.getBeltColor())
                .phone(user.getPhone())
                .build();
    }
}
