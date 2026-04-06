package com.rolling.api.domain.user.dto;

import com.rolling.api.domain.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "사용자 설정 응답")
public class UserSettingsResponse {

    @Schema(description = "푸시 알림 수신 여부", example = "true")
    private Boolean pushNotificationEnabled;

    public static UserSettingsResponse from(User user) {
        return UserSettingsResponse.builder()
                .pushNotificationEnabled(user.getPushNotificationEnabled())
                .build();
    }
}
