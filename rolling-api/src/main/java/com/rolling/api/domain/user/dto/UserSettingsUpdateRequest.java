package com.rolling.api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "사용자 설정 수정 요청")
public class UserSettingsUpdateRequest {

    @NotNull(message = "pushNotificationEnabled는 비어 있을 수 없습니다")
    @Schema(description = "푸시 알림 수신 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean pushNotificationEnabled;
}
