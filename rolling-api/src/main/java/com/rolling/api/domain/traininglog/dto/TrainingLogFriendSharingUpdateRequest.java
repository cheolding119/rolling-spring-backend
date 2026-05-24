package com.rolling.api.domain.traininglog.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class TrainingLogFriendSharingUpdateRequest {

    @NotNull(message = "shareWithFriends는 비어 있을 수 없습니다")
    private Boolean shareWithFriends;
}
