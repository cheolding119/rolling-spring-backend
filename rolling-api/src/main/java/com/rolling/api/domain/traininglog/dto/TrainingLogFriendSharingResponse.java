package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.UserTrainingLogShareSetting;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TrainingLogFriendSharingResponse {

    private Boolean shareWithFriends;
    private LocalDateTime updatedAt;

    public static TrainingLogFriendSharingResponse from(UserTrainingLogShareSetting setting) {
        return TrainingLogFriendSharingResponse.builder()
                .shareWithFriends(setting.isShareWithFriends())
                .updatedAt(setting.getUpdatedAt())
                .build();
    }

    public static TrainingLogFriendSharingResponse defaultPrivate() {
        return TrainingLogFriendSharingResponse.builder()
                .shareWithFriends(false)
                .updatedAt(null)
                .build();
    }
}
