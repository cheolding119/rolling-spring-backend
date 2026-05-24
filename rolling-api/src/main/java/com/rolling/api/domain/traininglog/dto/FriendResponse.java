package com.rolling.api.domain.traininglog.dto;

import com.rolling.api.domain.traininglog.entity.Friendship;
import com.rolling.api.domain.user.entity.BeltColor;
import com.rolling.api.domain.user.entity.User;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FriendResponse {

    private Long userId;
    private String nickname;
    private BeltColor beltColor;
    private String affiliation;
    private LocalDateTime friendedAt;

    public static FriendResponse from(Friendship friendship) {
        return from(friendship.getFriendUser(), friendship.getFriendedAt());
    }

    public static FriendResponse from(User friendUser, LocalDateTime friendedAt) {
        return FriendResponse.builder()
                .userId(friendUser.getId())
                .nickname(friendUser.getNickname())
                .beltColor(friendUser.getBeltColor())
                .affiliation(friendUser.getAffiliation())
                .friendedAt(friendedAt)
                .build();
    }
}
