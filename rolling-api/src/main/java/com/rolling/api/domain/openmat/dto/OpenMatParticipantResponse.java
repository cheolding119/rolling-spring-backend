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

    @Schema(description = "참가자 이름", example = "rolling_user")
    private String name;

    @Schema(description = "참가자 소속", example = "롤링짐 강남", nullable = true)
    private String affiliation;

    @Schema(description = "참가자 벨트", example = "BLUE")
    private BeltColor beltColor;

    @Schema(description = "참가자 그랄 수", example = "2", nullable = true)
    private Integer stripeCount;

    public static OpenMatParticipantResponse from(User user) {
        return OpenMatParticipantResponse.builder()
                .userId(user.getId())
                .name(user.getNickname())
                .affiliation(user.getAffiliation())
                .beltColor(user.getBeltColor())
                .stripeCount(user.getStripeCount())
                .build();
    }
}
