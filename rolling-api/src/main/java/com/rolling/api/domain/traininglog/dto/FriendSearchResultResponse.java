package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FriendSearchResultResponse {

    private Long userId;
    private String nickname;
    private String affiliation;
    private BeltColor beltColor;

    public static FriendSearchResultResponse from(User user) {
        return FriendSearchResultResponse.builder()
                .userId(user.getId())
                .nickname(user.getNickname())
                .affiliation(user.getAffiliation())
                .beltColor(user.getBeltColor())
                .build();
    }
}
