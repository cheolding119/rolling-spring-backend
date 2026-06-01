package com.rolling.api.domain.user.dto;

import com.rolling.api.domain.user.entity.BeltColor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.Getter;

@Getter
@Schema(description = "내 정보 수정 요청")
public class UserUpdateRequest {

    @Schema(description = "닉네임", example = "rolling_user")
    private String nickname;

    @Schema(description = "소속 체육관", example = "롤링짐 강남", nullable = true)
    private String affiliation;

    @Schema(
            description = "주짓수 벨트 색상",
            example = "BLUE",
            allowableValues = {
                    "WHITE",
                    "GRAY_WHITE",
                    "GRAY",
                    "GRAY_BLACK",
                    "YELLOW_WHITE",
                    "YELLOW",
                    "YELLOW_BLACK",
                    "ORANGE_WHITE",
                    "ORANGE",
                    "ORANGE_BLACK",
                    "GREEN_WHITE",
                    "GREEN",
                    "GREEN_BLACK",
                    "BLUE",
                    "PURPLE",
                    "BROWN",
                    "BLACK"
            }
    )
    private BeltColor beltColor;

    @Min(value = 0, message = "stripeCount는 0 이상이어야 합니다")
    @Schema(description = "그랄 수", example = "2", nullable = true)
    private Integer stripeCount;
}
