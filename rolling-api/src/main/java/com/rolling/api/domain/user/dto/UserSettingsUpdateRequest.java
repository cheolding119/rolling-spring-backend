package com.rolling.api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import lombok.Getter;

@Getter
@Schema(description = "사용자 설정 수정 요청")
public class UserSettingsUpdateRequest {

    @Schema(description = "푸시 알림 수신 여부", example = "false")
    private Boolean pushNotificationEnabled;

    @Schema(description = "내 훈련일지 상세의 좋아요/댓글 노출 여부", example = "false")
    private Boolean showOwnReactions;

    @AssertTrue(message = "수정할 설정값이 필요합니다")
    public boolean hasAnyUpdate() {
        return pushNotificationEnabled != null || showOwnReactions != null;
    }
}
